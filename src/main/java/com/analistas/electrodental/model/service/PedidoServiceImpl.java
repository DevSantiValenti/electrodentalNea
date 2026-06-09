package com.analistas.electrodental.model.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.CanalVenta;
import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.EstadoPago;
import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.Pago;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.repository.IClienteRepository;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.model.repository.IPedidoRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;

@Service
public class PedidoServiceImpl implements IPedidoService {

	private final IPedidoRepository pedidoRepository;
	private final IProductoRepository productoRepository;
	private final IClienteRepository clienteRepository;
	private final IPagoRepository pagoRepository;
	private final IStockService stockService;

	public PedidoServiceImpl(
			IPedidoRepository pedidoRepository,
			IProductoRepository productoRepository,
			IClienteRepository clienteRepository,
			IPagoRepository pagoRepository,
			IStockService stockService) {
		this.pedidoRepository = pedidoRepository;
		this.productoRepository = productoRepository;
		this.clienteRepository = clienteRepository;
		this.pagoRepository = pagoRepository;
		this.stockService = stockService;
	}

	@Override
	@Transactional
	public Pedido crearPedidoWeb(Cliente cliente, DireccionEnvio direccionEnvio, CarritoDTO carrito) {
		return crearPedidoWeb(cliente, direccionEnvio, carrito, null, BigDecimal.ZERO);
	}

	@Override
	@Transactional
	public Pedido crearPedidoWeb(Cliente cliente, DireccionEnvio direccionEnvio, CarritoDTO carrito, String metodoEntrega, BigDecimal costoEnvio) {
		if (carrito == null || carrito.items().isEmpty()) {
			throw new IllegalArgumentException("El carrito no puede estar vacio");
		}

		String dniCuit = normalizarDniCuit(cliente.getDniCuit());
		if (dniCuit.isBlank()) {
			throw new IllegalArgumentException("El DNI del cliente es obligatorio");
		}
		cliente.setDniCuit(dniCuit);
		Cliente clientePersistido = clienteRepository.findByDniCuitNormalizado(dniCuit)
				.map(existente -> actualizarCliente(existente, cliente))
				.orElseGet(() -> clienteRepository.save(cliente));
		direccionEnvio.setCliente(clientePersistido);

		Pedido pedido = new Pedido();
		pedido.setCliente(clientePersistido);
		pedido.setDireccionEnvio(direccionEnvio);
		pedido.setCanal(CanalVenta.WEB);
		pedido.setEstadoPedido(EstadoPedido.PENDIENTE_PAGO);
		pedido.setMetodoEntrega(metodoEntrega == null || metodoEntrega.isBlank() ? "ANDREANI" : metodoEntrega);
		pedido.setCodigoCompra(generarCodigoCompra(pedido.getMetodoEntrega()));
		pedido.setCostoEnvio(costoEnvio == null ? BigDecimal.ZERO : costoEnvio);

		Pago pago = new Pago();
		pago.setEstadoPago(EstadoPago.PENDIENTE);
		pago.setExternalReference("PEDIDO-" + System.currentTimeMillis());
		pedido.setPago(pago);

		carrito.items().forEach(itemCarrito -> {
			Producto producto = productoRepository.findById(itemCarrito.productoId())
					.orElseThrow(() -> new IllegalArgumentException("Producto inexistente: " + itemCarrito.productoId()));

			PedidoItem item = new PedidoItem();
			item.setProducto(producto);
			item.setNombreSnapshot(producto.getNombre());
			item.setPrecioUnitarioSnapshot(producto.getPrecio());
			item.setCantidad(itemCarrito.cantidad());
			item.calcularSubtotal();
			pedido.agregarItem(item);
			stockService.reservarStockWeb(producto, itemCarrito.cantidad(), pago.getExternalReference());
		});

		return pedidoRepository.save(pedido);
	}

	private String generarCodigoCompra(String metodoEntrega) {
		String prefijo = switch (metodoEntrega == null ? "" : metodoEntrega) {
			case "SUCURSAL" -> "RET";
			case "VENDEDOR" -> "COO";
			default -> "WEB";
		};
		return prefijo + "-" + System.currentTimeMillis();
	}

	private Cliente actualizarCliente(Cliente existente, Cliente datos) {
		existente.setNombre(datos.getNombre());
		existente.setApellidoRazonSocial(datos.getApellidoRazonSocial());
		existente.setTelefono(datos.getTelefono());
		existente.setDniCuit(datos.getDniCuit());
		existente.setEmail(datos.getEmail());
		return clienteRepository.save(existente);
	}

	private String normalizarDniCuit(String valor) {
		return valor == null ? "" : valor.replaceAll("[^0-9]", "").trim();
	}

	@Override
	@Transactional
	public Pedido marcarPagado(String externalReference, String paymentId) {
		return actualizarPagoMercadoPago(externalReference, paymentId, "approved");
	}

	@Override
	@Transactional
	public Pedido actualizarPagoMercadoPago(String externalReference, String paymentId, String status) {
		Pago pago = pagoRepository.findByExternalReference(externalReference)
				.orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + externalReference));
		Pedido pedido = pago.getPedido();
		boolean pagoYaAprobado = pago.getEstadoPago() == EstadoPago.APROBADO || pedido.getEstadoPedido() == EstadoPedido.PAGADO;
		boolean reservaActiva = pago.getEstadoPago() == EstadoPago.PENDIENTE && pedido.getEstadoPedido() == EstadoPedido.PENDIENTE_PAGO;
		pago.setPaymentId(paymentId);
		switch (status == null ? "" : status.toLowerCase()) {
			case "approved", "200" -> {
				pago.setEstadoPago(EstadoPago.APROBADO);
				pago.setFechaAprobacion(LocalDateTime.now());
				pedido.setEstadoPedido(EstadoPedido.PAGADO);
				if (!pagoYaAprobado) {
					pedido.setFechaPago(LocalDateTime.now());
				}
			}
			case "pending", "in_process" -> {
				pago.setEstadoPago(EstadoPago.PENDIENTE);
				pedido.setEstadoPedido(EstadoPedido.PENDIENTE_PAGO);
			}
			case "rejected", "cancelled", "canceled", "failure", "failed" -> {
				if (reservaActiva) {
					liberarReservaWeb(pedido);
				}
				pago.setEstadoPago(EstadoPago.RECHAZADO);
				pedido.setEstadoPedido(EstadoPedido.CANCELADO);
				pedido.setFechaCancelacion(LocalDateTime.now());
			}
			case "refunded", "charged_back" -> {
				pago.setEstadoPago(EstadoPago.REEMBOLSADO);
				pedido.setEstadoPedido(EstadoPedido.CANCELADO);
				pedido.setFechaCancelacion(LocalDateTime.now());
			}
			default -> {
				pago.setEstadoPago(EstadoPago.PENDIENTE);
				pedido.setEstadoPedido(EstadoPedido.PENDIENTE_PAGO);
			}
		}
		return pedidoRepository.save(pedido);
	}

	private void liberarReservaWeb(Pedido pedido) {
		String referencia = pedido.getPago() != null ? pedido.getPago().getExternalReference() : "PEDIDO-" + pedido.getId();
		pedido.getItems().forEach(item -> stockService.liberarReservaWeb(
				item.getProducto(),
				item.getCantidad(),
				referencia));
	}

	@Override
	@Transactional
	public Pedido cancelarPedido(Long pedidoId, String motivo) {
		Pedido pedido = pedidoRepository.findById(pedidoId)
				.orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));
		boolean reservaActiva = pedido.getPago() != null
				&& pedido.getPago().getEstadoPago() == EstadoPago.PENDIENTE
				&& pedido.getEstadoPedido() == EstadoPedido.PENDIENTE_PAGO;
		pedido.setEstadoPedido(EstadoPedido.CANCELADO);
		pedido.setFechaCancelacion(LocalDateTime.now());
		if (pedido.getPago() != null) {
			if (reservaActiva) {
				liberarReservaWeb(pedido);
			}
			pedido.getPago().setEstadoPago(EstadoPago.CANCELADO);
		}
		return pedidoRepository.save(pedido);
	}
}
