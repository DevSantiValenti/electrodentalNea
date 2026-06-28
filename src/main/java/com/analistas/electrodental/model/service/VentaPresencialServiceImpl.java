package com.analistas.electrodental.model.service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.MetodoPagoVenta;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.VentaPresencial;
import com.analistas.electrodental.model.domain.VentaPresencialItem;
import com.analistas.electrodental.model.domain.dto.VentaPresencialItemRequestDTO;
import com.analistas.electrodental.model.domain.dto.VentaPresencialRequestDTO;
import com.analistas.electrodental.model.repository.IClienteRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.IVentaPresencialRepository;

@Service
public class VentaPresencialServiceImpl implements IVentaPresencialService {

	private final IVentaPresencialRepository ventaPresencialRepository;
	private final IProductoRepository productoRepository;
	private final IClienteRepository clienteRepository;
	private final IStockService stockService;

	public VentaPresencialServiceImpl(
			IVentaPresencialRepository ventaPresencialRepository,
			IProductoRepository productoRepository,
			IClienteRepository clienteRepository,
			IStockService stockService) {
		this.ventaPresencialRepository = ventaPresencialRepository;
		this.productoRepository = productoRepository;
		this.clienteRepository = clienteRepository;
		this.stockService = stockService;
	}

	@Override
	@Transactional
	public VentaPresencial registrarVenta(VentaPresencialRequestDTO request) {
		validarItems(request);

		VentaPresencial venta = new VentaPresencial();
		venta.setCliente(resolverCliente(request));
		venta.setMetodoPago(request.metodoPago() == null ? MetodoPagoVenta.EFECTIVO : request.metodoPago());
		venta.setUsuarioAdmin(request.usuarioAdmin());
		venta.setObservaciones(request.observaciones());

		request.items().forEach(itemRequest -> agregarItemNuevo(venta, itemRequest, "VENTA_FISICA"));

		return ventaPresencialRepository.save(venta);
	}

	@Override
	@Transactional(readOnly = true)
	public VentaPresencial obtenerVenta(Long id) {
		return buscarVenta(id);
	}

	@Override
	@Transactional
	public VentaPresencial actualizarVenta(Long id, VentaPresencialRequestDTO request) {
		validarItems(request);

		VentaPresencial venta = buscarVenta(id);
		Map<Long, VentaPresencialItem> itemsAnteriores = venta.getItems().stream()
				.collect(Collectors.toMap(item -> item.getProducto().getId(), Function.identity(), (primero, segundo) -> primero));

		restaurarStockVenta(venta, "EDICION_VENTA_FISICA #" + id);

		venta.setCliente(resolverCliente(request));
		venta.setMetodoPago(request.metodoPago() == null ? MetodoPagoVenta.EFECTIVO : request.metodoPago());
		venta.setUsuarioAdmin(request.usuarioAdmin());
		venta.setObservaciones(request.observaciones());
		venta.getItems().clear();

		request.items().forEach(itemRequest -> agregarItemActualizado(
				venta,
				itemRequest,
				itemsAnteriores.get(itemRequest.productoId()),
				"EDICION_VENTA_FISICA #" + id));

		return ventaPresencialRepository.save(venta);
	}

	@Override
	@Transactional
	public void eliminarVenta(Long id) {
		VentaPresencial venta = buscarVenta(id);
		restaurarStockVenta(venta, "ELIMINACION_VENTA_FISICA #" + id);
		ventaPresencialRepository.delete(venta);
	}

	private VentaPresencial buscarVenta(Long id) {
		return ventaPresencialRepository.findDetalleById(id)
				.orElseThrow(() -> new IllegalArgumentException("Venta presencial inexistente: " + id));
	}

	private void validarItems(VentaPresencialRequestDTO request) {
		if (request.items().isEmpty()) {
			throw new IllegalArgumentException("La venta presencial debe tener al menos un item");
		}
		request.items().forEach(itemRequest -> {
			if (itemRequest.productoId() == null || itemRequest.cantidad() == null || itemRequest.cantidad() < 1) {
				throw new IllegalArgumentException("Cada linea de venta debe tener producto y cantidad mayor a cero");
			}
		});
	}

	private void agregarItemNuevo(VentaPresencial venta, VentaPresencialItemRequestDTO itemRequest, String referencia) {
		Producto producto = productoRepository.findById(itemRequest.productoId())
				.orElseThrow(() -> new IllegalArgumentException("Producto inexistente: " + itemRequest.productoId()));
		stockService.registrarVentaFisica(producto, itemRequest.cantidad(), referencia);

		VentaPresencialItem item = new VentaPresencialItem();
		item.setProducto(producto);
		item.setNombreSnapshot(producto.getNombre());
		item.setCantidad(itemRequest.cantidad());
		item.setPrecioUnitarioSnapshot(producto.getPrecio());
		item.calcularSubtotal();
		venta.agregarItem(item);
	}

	private void agregarItemActualizado(
			VentaPresencial venta,
			VentaPresencialItemRequestDTO itemRequest,
			VentaPresencialItem itemAnterior,
			String referencia) {
		Producto producto = productoRepository.findById(itemRequest.productoId())
				.orElseThrow(() -> new IllegalArgumentException("Producto inexistente: " + itemRequest.productoId()));
		stockService.registrarVentaFisica(producto, itemRequest.cantidad(), referencia);

		VentaPresencialItem item = new VentaPresencialItem();
		item.setProducto(producto);
		item.setNombreSnapshot(itemAnterior != null ? itemAnterior.getNombreSnapshot() : producto.getNombre());
		item.setCantidad(itemRequest.cantidad());
		item.setPrecioUnitarioSnapshot(itemAnterior != null ? itemAnterior.getPrecioUnitarioSnapshot() : producto.getPrecio());
		item.calcularSubtotal();
		venta.agregarItem(item);
	}

	private void restaurarStockVenta(VentaPresencial venta, String referencia) {
		venta.getItems().forEach(item -> {
			Producto producto = item.getProducto();
			stockService.ajustarStock(
					producto,
					producto.getStockWeb(),
					producto.getStockFisico() + item.getCantidad(),
					referencia);
		});
	}

	private Cliente resolverCliente(VentaPresencialRequestDTO request) {
		String dniCuit = normalizarDniCuit(request.clienteDniCuit());
		if (dniCuit.isBlank()) {
			throw new IllegalArgumentException("El DNI del cliente es obligatorio");
		}
		Cliente cliente = clienteRepository.findByDniCuitNormalizado(dniCuit)
				.orElseGet(Cliente::new);
		cliente.setDniCuit(dniCuit);
		cliente.setNombre(valorConDefault(request.clienteNombre(), "Cliente " + dniCuit));
		cliente.setApellidoRazonSocial(valorConDefault(request.clienteApellidoRazonSocial(), cliente.getApellidoRazonSocial()));
		cliente.setTelefono(valorConDefault(request.clienteTelefono(), cliente.getTelefono()));
		cliente.setEmail(valorConDefault(request.clienteEmail(), emailPlaceholder(dniCuit)));
		return clienteRepository.save(cliente);
	}

	private String normalizarDniCuit(String valor) {
		return valor == null ? "" : valor.replaceAll("[^0-9]", "").trim();
	}

	private String valorConDefault(String valor, String defaultValue) {
		return valor == null || valor.isBlank() ? defaultValue : valor.trim();
	}

	private String emailPlaceholder(String dniCuit) {
		return "cliente-" + dniCuit + "@electrodental.local";
	}
}
