package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.OcaCotizacionResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaSucursalDTO;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.service.IOcaService;

@RestController
public class OcaEnvioController {

	private final IProductoRepository productoRepository;
	private final IOcaService ocaService;

	public OcaEnvioController(IProductoRepository productoRepository, IOcaService ocaService) {
		this.productoRepository = productoRepository;
		this.ocaService = ocaService;
	}

	@GetMapping("/api/oca/productos/{productoId}/opciones")
	public OcaOpcionesEnvioProductoDTO opcionesProducto(
			@PathVariable Long productoId,
			@RequestParam String codigoPostal,
			@RequestParam(defaultValue = "1") Integer cantidad) {
		if (!StringUtils.hasText(codigoPostal)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresá un código postal.");
		}
		Producto producto = productoRepository.findById(productoId)
				.filter(item -> Boolean.TRUE.equals(item.getActivo()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
		Pedido pedido = crearPedidoTemporal(producto, cantidad, codigoPostal);
		try {
			List<OcaSucursalDTO> sucursales = ocaService.obtenerSucursales(codigoPostal);
			OcaCotizacionResponseDTO domicilio = ocaService.cotizarDomicilio(pedido);
			OcaCotizacionResponseDTO sucursal = sucursales.isEmpty() ? null : ocaService.cotizarSucursal(pedido);
			return new OcaOpcionesEnvioProductoDTO(tarifa(domicilio), tarifa(sucursal), sucursales);
		} catch (RuntimeException ex) {
			OcaTarifaDTO noDisponible = new OcaTarifaDTO(false, BigDecimal.ZERO.setScale(2), null, "OCA no está disponible en este momento.");
			return new OcaOpcionesEnvioProductoDTO(noDisponible, noDisponible, List.of());
		}
	}

	private Pedido crearPedidoTemporal(Producto producto, Integer cantidad, String codigoPostal) {
		Pedido pedido = new Pedido();
		DireccionEnvio direccion = new DireccionEnvio();
		direccion.setCalle("Sin definir");
		direccion.setNumero("0");
		direccion.setCiudad("Sin definir");
		direccion.setProvincia("Sin definir");
		direccion.setCodigoPostal(codigoPostal);
		direccion.setPais("AR");
		pedido.setDireccionEnvio(direccion);

		PedidoItem item = new PedidoItem();
		item.setProducto(producto);
		item.setCantidad(Math.max(1, cantidad == null ? 1 : cantidad));
		item.setPrecioUnitarioSnapshot(producto.getPrecio());
		item.setNombreSnapshot(producto.getNombre());
		item.calcularSubtotal();
		pedido.agregarItem(item);
		return pedido;
	}

	private OcaTarifaDTO tarifa(OcaCotizacionResponseDTO cotizacion) {
		if (cotizacion == null) {
			return new OcaTarifaDTO(false, BigDecimal.ZERO.setScale(2), null, "No hay tarifa disponible para sucursal OCA.");
		}
		return new OcaTarifaDTO(
				cotizacion.cotizada(),
				(cotizacion.costo() == null ? BigDecimal.ZERO : cotizacion.costo()).setScale(2, RoundingMode.HALF_UP),
				cotizacion.plazoEntrega(),
				cotizacion.mensaje());
	}

	public record OcaOpcionesEnvioProductoDTO(
			OcaTarifaDTO domicilio,
			OcaTarifaDTO sucursal,
			List<OcaSucursalDTO> sucursales) {
	}

	public record OcaTarifaDTO(
			boolean disponible,
			BigDecimal costo,
			String plazoEntrega,
			String mensaje) {
	}
}
