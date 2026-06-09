package com.analistas.electrodental.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.MetodoPagoVenta;
import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.VentaPresencial;
import com.analistas.electrodental.model.domain.VentaPresencialItem;
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
		if (request.items().isEmpty()) {
			throw new IllegalArgumentException("La venta presencial debe tener al menos un item");
		}

		VentaPresencial venta = new VentaPresencial();
		venta.setCliente(resolverCliente(request));
		venta.setMetodoPago(request.metodoPago() == null ? MetodoPagoVenta.EFECTIVO : request.metodoPago());
		venta.setUsuarioAdmin(request.usuarioAdmin());
		venta.setObservaciones(request.observaciones());

		request.items().forEach(itemRequest -> {
			if (itemRequest.productoId() == null || itemRequest.cantidad() == null || itemRequest.cantidad() < 1) {
				throw new IllegalArgumentException("Cada linea de venta debe tener producto y cantidad mayor a cero");
			}
			Producto producto = productoRepository.findById(itemRequest.productoId())
					.orElseThrow(() -> new IllegalArgumentException("Producto inexistente: " + itemRequest.productoId()));
			stockService.registrarVentaFisica(producto, itemRequest.cantidad(), "VENTA_FISICA");

			VentaPresencialItem item = new VentaPresencialItem();
			item.setProducto(producto);
			item.setNombreSnapshot(producto.getNombre());
			item.setCantidad(itemRequest.cantidad());
			item.setPrecioUnitarioSnapshot(producto.getPrecio());
			item.calcularSubtotal();
			venta.agregarItem(item);
		});

		return ventaPresencialRepository.save(venta);
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
