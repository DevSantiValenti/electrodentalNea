package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.EstadoPago;
import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.Pago;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.ProveedorPago;
import com.analistas.electrodental.model.domain.TipoAplicacionDescuento;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.dto.CarritoItemDTO;
import com.analistas.electrodental.model.domain.dto.DescuentoAplicadoDTO;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPaymentDataDTO;
import com.analistas.electrodental.model.domain.dto.OcaCreacionEnvioResponseDTO;
import com.analistas.electrodental.model.domain.dto.ResultadoDescuentoDTO;
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
	@Mock
	IDescuentoService descuentoService;

	PedidoServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new PedidoServiceImpl(
				pedidoRepository,
				productoRepository,
				clienteRepository,
				pagoRepository,
				stockService,
				ocaService,
				descuentoService);
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
	void aprobarPagoConDescuentoRegistraUsoDelCupon() {
		Pedido pedido = pedido("SUCURSAL");
		pedido.setCodigoDescuento("DENTAL10");
		pedido.setDescuentoAplicado(BigDecimal.ONE);
		when(pagoRepository.findByExternalReference("EXT-1")).thenReturn(Optional.of(pedido.getPago()));

		service.actualizarPagoMercadoPago(payment("approved"));

		verify(descuentoService).registrarUso("DENTAL10");
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
		verify(descuentoService, never()).registrarUso(any());
		verify(ocaService).crearEnvio(pedido);
	}

	@Test
	void confirmarTransferenciaMarcaPagadoYCreaEnvioOca() {
		Pedido pedido = pedidoTransferencia("OCA");
		pedido.setCodigoDescuento("DENTAL10");
		when(pedidoRepository.findDetalleById(10L)).thenReturn(Optional.of(pedido));

		service.confirmarTransferencia(10L);

		assertThat(pedido.getPago().getEstadoPago()).isEqualTo(EstadoPago.APROBADO);
		assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
		assertThat(pedido.getFechaPago()).isNotNull();
		verify(descuentoService).registrarUso("DENTAL10");
		verify(ocaService).crearEnvio(pedido);
	}

	@Test
	void confirmarTransferenciaAdvierteSiNoSePudoCrearEnvioOca() {
		Pedido pedido = pedidoTransferencia("OCA");
		when(pedidoRepository.findDetalleById(10L)).thenReturn(Optional.of(pedido));
		when(ocaService.crearEnvio(pedido)).thenReturn(new OcaCreacionEnvioResponseDTO(
				false,
				null,
				null,
				"Config faltante",
				null,
				null));

		Pedido resultado = service.confirmarTransferencia(10L);

		assertThat(resultado.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
		assertThat(resultado.getAdvertenciaOperacion()).contains("no se pudo crear el envío OCA");
	}

	@Test
	void crearPedidoTransferenciaConCuponNoAcumulaDescuentoTransferencia() {
		Producto producto = productoDisponible(1L);
		CarritoDTO carritoConCupon = carritoConCupon();
		when(descuentoService.aplicar(any(CarritoDTO.class), any())).thenReturn(new ResultadoDescuentoDTO(true, "OK", carritoConCupon));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(clienteRepository.findByDniCuitNormalizado("12345678")).thenReturn(Optional.empty());
		when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Pedido pedido = service.crearPedidoWebTransferencia(
				clienteWeb(),
				direccionWeb(),
				carritoConCupon,
				"SUCURSAL",
				BigDecimal.ZERO);

		assertThat(pedido.getCodigoDescuento()).isEqualTo("DENTAL15");
		assertThat(pedido.getDescuentoAplicado()).isEqualByComparingTo("15.00");
		assertThat(pedido.getTotal()).isEqualByComparingTo("85.00");
		assertThat(pedido.getPago().getTransactionAmount()).isEqualByComparingTo("85.00");
	}

	@Test
	void crearPedidoTransferenciaConOfertaNoAcumulaDescuentoTransferencia() {
		Producto producto = productoDisponible(1L);
		producto.setOferta(true);
		producto.setPorcentajeOferta(new BigDecimal("20"));
		CarritoItemDTO item = new CarritoItemDTO(
				1L,
				"producto-test",
				"Producto test",
				"/img/test.png",
				new BigDecimal("80.00"),
				1,
				new BigDecimal("80.00"),
				10,
				false,
				true);
		CarritoDTO carrito = new CarritoDTO(List.of(item), new BigDecimal("80.00"), 1);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(clienteRepository.findByDniCuitNormalizado("12345678")).thenReturn(Optional.empty());
		when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Pedido pedido = service.crearPedidoWebTransferencia(
				clienteWeb(),
				direccionWeb(),
				carrito,
				"SUCURSAL",
				BigDecimal.ZERO);

		assertThat(pedido.getDescuentoAplicado()).isZero();
		assertThat(pedido.getTotal()).isEqualByComparingTo("80.00");
		assertThat(pedido.getPago().getTransactionAmount()).isEqualByComparingTo("80.00");
	}

	@Test
	void comprobanteTransferenciaInvalidoCancelaYLiberaReserva() {
		Pedido pedido = pedidoTransferencia("SUCURSAL");
		when(pedidoRepository.findDetalleById(10L)).thenReturn(Optional.of(pedido));

		service.rechazarTransferencia(10L);

		assertThat(pedido.getPago().getEstadoPago()).isEqualTo(EstadoPago.RECHAZADO);
		assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
		verify(stockService).liberarReservaWeb(pedido.getItems().get(0).getProducto(), 2, "TRF-1");
		verify(ocaService, never()).crearEnvio(any());
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

	private Pedido pedidoTransferencia(String metodoEntrega) {
		Pedido pedido = pedido(metodoEntrega);
		pedido.setEstadoPedido(EstadoPedido.PENDIENTE_TRANSFERENCIA);
		pedido.getPago().setProveedor(ProveedorPago.TRANSFERENCIA_BANCARIA);
		pedido.getPago().setExternalReference("TRF-1");
		return pedido;
	}

	private Producto productoDisponible(Long id) {
		Producto producto = new Producto();
		producto.setId(id);
		producto.setNombre("Producto test");
		producto.setPrecio(new BigDecimal("100.00"));
		producto.setStockWeb(10);
		producto.setStockFisico(10);
		return producto;
	}

	private CarritoDTO carritoConCupon() {
		CarritoItemDTO item = new CarritoItemDTO(
				1L,
				"producto-test",
				"Producto test",
				"/img/test.png",
				new BigDecimal("100.00"),
				1,
				new BigDecimal("100.00"),
				10,
				false,
				false,
				new BigDecimal("15.00"),
				new BigDecimal("85.00"));
		DescuentoAplicadoDTO descuento = new DescuentoAplicadoDTO(
				1L,
				"DENTAL15",
				new BigDecimal("15.00"),
				TipoAplicacionDescuento.CARRITO,
				new BigDecimal("100.00"),
				new BigDecimal("15.00"),
				"15% - Todo el carrito");
		return new CarritoDTO(List.of(item), new BigDecimal("100.00"), 1, descuento);
	}

	private Cliente clienteWeb() {
		Cliente cliente = new Cliente();
		cliente.setNombre("Cliente");
		cliente.setApellidoRazonSocial("Web");
		cliente.setEmail("cliente@demo.com");
		cliente.setTelefono("123");
		cliente.setDniCuit("12.345.678");
		return cliente;
	}

	private DireccionEnvio direccionWeb() {
		DireccionEnvio direccion = new DireccionEnvio();
		direccion.setCalle("Calle");
		direccion.setNumero("123");
		direccion.setCiudad("Resistencia");
		direccion.setProvincia("Chaco");
		direccion.setCodigoPostal("3500");
		return direccion;
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
