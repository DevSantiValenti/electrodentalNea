package com.analistas.electrodental.model.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.dto.CarritoItemDTO;

@Service
public class CarritoServiceImpl implements ICarritoService {

	@Override
	public CarritoDTO nuevoCarrito() {
		return new CarritoDTO();
	}

	@Override
	public CarritoDTO agregarProducto(CarritoDTO carrito, Producto producto, Integer cantidad) {
		List<CarritoItemDTO> items = new ArrayList<>(normalizar(carrito).items());
		int stockDisponible = stockDisponible(producto);
		int cantidadFinal = limitarCantidad(cantidad == null || cantidad < 1 ? 1 : cantidad, stockDisponible);
		if (cantidadFinal < 1) {
			return recalcular(items);
		}
		boolean actualizado = false;

		for (int i = 0; i < items.size(); i++) {
			CarritoItemDTO item = items.get(i);
			if (item.productoId().equals(producto.getId())) {
				int nuevaCantidad = limitarCantidad(item.cantidad() + cantidadFinal, stockDisponible);
				items.set(i, crearItem(producto, nuevaCantidad));
				actualizado = true;
				break;
			}
		}

		if (!actualizado) {
			items.add(crearItem(producto, cantidadFinal));
		}

		return recalcular(items);
	}

	@Override
	public CarritoDTO actualizarCantidad(CarritoDTO carrito, Long productoId, Integer cantidad) {
		if (cantidad == null || cantidad < 1) {
			return quitarProducto(carrito, productoId);
		}

		List<CarritoItemDTO> items = normalizar(carrito).items().stream()
				.map(item -> item.productoId().equals(productoId)
						? actualizarItemDesdeStockActual(item, cantidad)
						: item)
				.filter(item -> item.cantidad() > 0)
				.toList();
		return recalcular(items);
	}

	@Override
	public CarritoDTO actualizarCantidad(CarritoDTO carrito, Producto producto, Integer cantidad) {
		if (cantidad == null || cantidad < 1) {
			return quitarProducto(carrito, producto.getId());
		}
		int cantidadFinal = limitarCantidad(cantidad, stockDisponible(producto));
		if (cantidadFinal < 1) {
			return quitarProducto(carrito, producto.getId());
		}

		List<CarritoItemDTO> items = normalizar(carrito).items().stream()
				.map(item -> item.productoId().equals(producto.getId()) ? crearItem(producto, cantidadFinal) : item)
				.toList();
		return recalcular(items);
	}

	@Override
	public CarritoDTO quitarProducto(CarritoDTO carrito, Long productoId) {
		List<CarritoItemDTO> items = normalizar(carrito).items().stream()
				.filter(item -> !item.productoId().equals(productoId))
				.toList();
		return recalcular(items);
	}

	private CarritoDTO normalizar(CarritoDTO carrito) {
		return carrito == null ? nuevoCarrito() : carrito;
	}

	private CarritoItemDTO crearItem(Producto producto, Integer cantidad) {
		BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
		return new CarritoItemDTO(
				producto.getId(),
				producto.getSlug(),
				producto.getNombre(),
				producto.getImagenPrincipal(),
				producto.getPrecio(),
				cantidad,
				subtotal,
				stockDisponible(producto));
	}

	private CarritoItemDTO actualizarItemDesdeStockActual(CarritoItemDTO item, Integer cantidad) {
		int stockDisponible = item.stockDisponible() == null ? cantidad : item.stockDisponible();
		int cantidadFinal = limitarCantidad(cantidad, stockDisponible);
		return new CarritoItemDTO(
				item.productoId(),
				item.slug(),
				item.nombre(),
				item.imagenPrincipal(),
				item.precioUnitario(),
				cantidadFinal,
				item.precioUnitario().multiply(BigDecimal.valueOf(cantidadFinal)),
				item.stockDisponible());
	}

	private int stockDisponible(Producto producto) {
		return producto.getStockWeb() == null ? 0 : Math.max(0, producto.getStockWeb());
	}

	private int limitarCantidad(Integer cantidad, int stockDisponible) {
		int solicitada = cantidad == null ? 1 : cantidad;
		return Math.min(Math.max(0, solicitada), stockDisponible);
	}

	private CarritoDTO recalcular(List<CarritoItemDTO> items) {
		BigDecimal subtotal = items.stream()
				.map(CarritoItemDTO::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		Integer cantidadTotal = items.stream()
				.map(CarritoItemDTO::cantidad)
				.reduce(0, Integer::sum);
		return new CarritoDTO(new ArrayList<>(items), subtotal, cantidadTotal);
	}
}
