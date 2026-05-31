package com.analistas.electrodental.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.MetodoPagoVenta;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.VentaPresencial;
import com.analistas.electrodental.model.domain.VentaPresencialItem;
import com.analistas.electrodental.model.domain.dto.VentaPresencialRequestDTO;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.IVentaPresencialRepository;

@Service
public class VentaPresencialServiceImpl implements IVentaPresencialService {

	private final IVentaPresencialRepository ventaPresencialRepository;
	private final IProductoRepository productoRepository;
	private final IStockService stockService;

	public VentaPresencialServiceImpl(
			IVentaPresencialRepository ventaPresencialRepository,
			IProductoRepository productoRepository,
			IStockService stockService) {
		this.ventaPresencialRepository = ventaPresencialRepository;
		this.productoRepository = productoRepository;
		this.stockService = stockService;
	}

	@Override
	@Transactional
	public VentaPresencial registrarVenta(VentaPresencialRequestDTO request) {
		if (request.items().isEmpty()) {
			throw new IllegalArgumentException("La venta presencial debe tener al menos un item");
		}

		VentaPresencial venta = new VentaPresencial();
		venta.setMetodoPago(request.metodoPago() == null ? MetodoPagoVenta.EFECTIVO : request.metodoPago());
		venta.setUsuarioAdmin(request.usuarioAdmin());
		venta.setObservaciones(request.observaciones());

		request.items().forEach(itemRequest -> {
			Producto producto = productoRepository.findById(itemRequest.productoId())
					.orElseThrow(() -> new IllegalArgumentException("Producto inexistente: " + itemRequest.productoId()));
			stockService.registrarVentaFisica(producto, itemRequest.cantidad(), "VENTA_FISICA");

			VentaPresencialItem item = new VentaPresencialItem();
			item.setProducto(producto);
			item.setCantidad(itemRequest.cantidad());
			item.setPrecioUnitarioSnapshot(producto.getPrecio());
			item.calcularSubtotal();
			venta.agregarItem(item);
		});

		return ventaPresencialRepository.save(venta);
	}
}
