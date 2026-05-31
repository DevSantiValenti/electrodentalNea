package com.analistas.electrodental.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.MovimientoStock;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.TipoMovimientoStock;
import com.analistas.electrodental.model.repository.IMovimientoStockRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;

@Service
public class StockServiceImpl implements IStockService {

	private final IProductoRepository productoRepository;
	private final IMovimientoStockRepository movimientoStockRepository;

	public StockServiceImpl(
			IProductoRepository productoRepository,
			IMovimientoStockRepository movimientoStockRepository) {
		this.productoRepository = productoRepository;
		this.movimientoStockRepository = movimientoStockRepository;
	}

	@Override
	@Transactional
	public void reservarStockWeb(Producto producto, Integer cantidad, String referencia) {
		validarCantidad(cantidad);
		if (!producto.tieneStockWeb(cantidad)) {
			throw new IllegalStateException("No hay stock web suficiente para " + producto.getNombre());
		}
		registrarMovimiento(producto, TipoMovimientoStock.RESERVA_WEB, cantidad,
				producto.getStockWeb() - cantidad, producto.getStockFisico(), referencia);
	}

	@Override
	@Transactional
	public void liberarReservaWeb(Producto producto, Integer cantidad, String referencia) {
		validarCantidad(cantidad);
		registrarMovimiento(producto, TipoMovimientoStock.LIBERACION_RESERVA, cantidad,
				producto.getStockWeb() + cantidad, producto.getStockFisico(), referencia);
	}

	@Override
	@Transactional
	public void registrarVentaWeb(Producto producto, Integer cantidad, String referencia) {
		validarCantidad(cantidad);
		if (!producto.tieneStockWeb(cantidad)) {
			throw new IllegalStateException("No hay stock web suficiente para " + producto.getNombre());
		}
		registrarMovimiento(producto, TipoMovimientoStock.VENTA_WEB, cantidad,
				producto.getStockWeb() - cantidad, producto.getStockFisico(), referencia);
	}

	@Override
	@Transactional
	public void registrarVentaFisica(Producto producto, Integer cantidad, String referencia) {
		validarCantidad(cantidad);
		if (!producto.tieneStockFisico(cantidad)) {
			throw new IllegalStateException("No hay stock fisico suficiente para " + producto.getNombre());
		}
		registrarMovimiento(producto, TipoMovimientoStock.VENTA_FISICA, cantidad,
				producto.getStockWeb(), producto.getStockFisico() - cantidad, referencia);
	}

	@Override
	@Transactional
	public void ajustarStock(Producto producto, Integer nuevoStockWeb, Integer nuevoStockFisico, String referencia) {
		if (nuevoStockWeb == null || nuevoStockFisico == null || nuevoStockWeb < 0 || nuevoStockFisico < 0) {
			throw new IllegalArgumentException("Los stocks no pueden ser negativos");
		}
		registrarMovimiento(producto, TipoMovimientoStock.AJUSTE, 0, nuevoStockWeb, nuevoStockFisico, referencia);
	}

	private void registrarMovimiento(
			Producto producto,
			TipoMovimientoStock tipo,
			Integer cantidad,
			Integer nuevoStockWeb,
			Integer nuevoStockFisico,
			String referencia) {

		Integer stockAnteriorWeb = producto.getStockWeb();
		Integer stockAnteriorFisico = producto.getStockFisico();

		producto.setStockWeb(nuevoStockWeb);
		producto.setStockFisico(nuevoStockFisico);
		productoRepository.save(producto);

		MovimientoStock movimiento = new MovimientoStock();
		movimiento.setProducto(producto);
		movimiento.setTipo(tipo);
		movimiento.setCantidad(cantidad);
		movimiento.setStockAnteriorWeb(stockAnteriorWeb);
		movimiento.setStockNuevoWeb(nuevoStockWeb);
		movimiento.setStockAnteriorFisico(stockAnteriorFisico);
		movimiento.setStockNuevoFisico(nuevoStockFisico);
		movimiento.setReferencia(referencia);
		movimientoStockRepository.save(movimiento);
	}

	private void validarCantidad(Integer cantidad) {
		if (cantidad == null || cantidad < 1) {
			throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
		}
	}
}
