package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.analistas.electrodental.model.domain.EstadoPago;
import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.Pago;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPaymentDataDTO;
import com.analistas.electrodental.model.repository.IClienteRepository;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.model.repository.IPedidoRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

	@Mock
	IPedidoRepository pedidoRepository;
	@Mock
	IProductoRepository productoRepository;
	@Mock
	IClienteRepository clienteRepository;
	@Mock
	IPagoRepository pagoRepository;
	@Mock
	IStockService stockService;
	@Mock
	IOcaService ocaService;

	PedidoServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new PedidoServiceImpl(
				pedidoRepository,
				productoRepository,
				clienteRepository,
				pagoRepository,
				stockService,
				ocaService);
		when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void aprobarPagoMarcaPedidoPagadoGuardaMetadataYCreaEnvioOca() {
		Pedido pedido = pedido("OCA");
		when(pagoRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(pedido.getPago()));

		service.actualizarPagoMercadoPago(payment("approved"));

		assertThat(pedido.getPago().getEstadoPago()).isEqualTo(EstadoPago.APROBADO);
		assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
		assertThat(pedido.getPago().getStatusDetail()).isEqualTo("accredited");
		assertThat(pedido.getPago().getPaymentMethodId()).isEqualTo("visa");
		assertThat(pedido.getPago().getPaymentTypeId()).isEqualTo("credit_card");
		assertThat(pedido.getPago().getTransactionAmount()).isEqualByComparingTo("123.45");
		verify(ocaService).crearEnvio(pedido);
	}

	@Test
	void rechazoCancelaPedidoYLiberaReserva() {
		Pedido pedido = pedido("SUCURSAL");
		when(pagoRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(pedido.getPago()));

		service.actualizarPagoMercadoPago("EXT-1", "PAY-1", "rejected");

		assertThat(pedido.getPago().getEstadoPago()).isEqualTo(EstadoPago.RECHAZADO);
		assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
		verify(stockService).liberarReservaWeb(pedido.getItems().get(0).getProducto(), 2, "EXT-1");
		verify(ocaService, never()).crearEnvio(any());
	}

	@Test
	void pendienteMantieneReservaYPedidoPendiente() {
		Pedido pedido = pedido("OCA");
		when(pagoRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(pedido.getPago()));

		service.actualizarPagoMercadoPago("EXT-1", "PAY-1", "pending");

		assertThat(pedido.getPago().getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
		assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);
		verify(stockService, never()).liberarReservaWeb(any(), any(), any());
		verify(ocaService, never()).crearEnvio(any());
	}

	@Test
	void webhookAprobadoDuplicadoNoLiberaReservaYReintentaOcaIdempotente() {
		Pedido pedido = pedido("OCA");
		pedido.getPago().setEstadoPago(EstadoPago.APROBADO);
		pedido.setEstadoPedido(EstadoPedido.PAGADO);
		when(pagoRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(pedido.getPago()));

		service.actualizarPagoMercadoPago("EXT-1", "PAY-1", "approved");

		assertThat(pedido.getPago().getEstadoPago()).isEqualTo(EstadoPago.APROBADO);
		assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
		verify(stockService, never()).liberarReservaWeb(any(), any(), any());
		verify(ocaService).crearEnvio(pedido);
	}

	private Pedido pedido(String metodoEntrega) {
		Producto producto = new Producto();
		producto.setNombre("Producto test");
		producto.setStockWeb(10);
		producto.setStockFisico(10);

		Pedido pedido = new Pedido();
		pedido.setMetodoEntrega(metodoEntrega);
		pedido.setEstadoPedido(EstadoPedido.PENDIENTE_PAGO);

		Pago pago = new Pago();
		pago.setExternalReference("EXT-1");
		pago.setEstadoPago(EstadoPago.PENDIENTE);
		pedido.setPago(pago);

		PedidoItem item = new PedidoItem();
		item.setProducto(producto);
		item.setCantidad(2);
		item.setPrecioUnitarioSnapshot(BigDecimal.TEN);
		item.setNombreSnapshot("Producto test");
		item.calcularSubtotal();
		pedido.agregarItem(item);
		return pedido;
	}

	private MercadoPagoPaymentDataDTO payment(String status) {
		return new MercadoPagoPaymentDataDTO(
				"EXT-1",
				"PAY-1",
				status,
				"accredited",
				"visa",
				"credit_card",
				new BigDecimal("123.45"));
	}
}
