package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.RestClient;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPaymentDataDTO;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPreferenceResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaCotizacionResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaSucursalDTO;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IMercadoPagoService;
import com.analistas.electrodental.model.service.IOcaService;
import com.analistas.electrodental.model.service.IPedidoService;
import com.analistas.electrodental.web.config.MercadoPagoProperties;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.JsonNode;

@Controller
public class CheckoutController {

	private static final String OCA_DOMICILIO = "DOMICILIO";
	private static final String OCA_SUCURSAL = "SUCURSAL";

	private final IPedidoService pedidoService;
	private final IMercadoPagoService mercadoPagoService;
	private final IProductoRepository productoRepository;
	private final IPagoRepository pagoRepository;
	private final MercadoPagoProperties mercadoPagoProperties;
	private final IConfiguracionTiendaService configuracionTiendaService;
	private final IOcaService ocaService;

	public CheckoutController(
			IPedidoService pedidoService,
			IMercadoPagoService mercadoPagoService,
			IProductoRepository productoRepository,
			IPagoRepository pagoRepository,
			MercadoPagoProperties mercadoPagoProperties,
			IConfiguracionTiendaService configuracionTiendaService,
			IOcaService ocaService) {
		this.pedidoService = pedidoService;
		this.mercadoPagoService = mercadoPagoService;
		this.productoRepository = productoRepository;
		this.pagoRepository = pagoRepository;
		this.mercadoPagoProperties = mercadoPagoProperties;
		this.configuracionTiendaService = configuracionTiendaService;
		this.ocaService = ocaService;
	}

	@GetMapping("/checkout/datos")
	public String datos(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de continuar.");
			return "redirect:/carrito";
		}
		model.addAttribute("pasoCheckout", 2);
		model.addAttribute("envioSeleccionado", session.getAttribute("envioSeleccionado"));
		model.addAttribute("codigoPostalDestino", session.getAttribute("codigoPostalDestino"));
		model.addAttribute("cotizacionOca", session.getAttribute("cotizacionOca"));
		model.addAttribute("cotizacionOcaDomicilio", session.getAttribute("cotizacionOcaDomicilio"));
		model.addAttribute("cotizacionOcaSucursal", session.getAttribute("cotizacionOcaSucursal"));
		model.addAttribute("ocaSucursales", session.getAttribute("ocaSucursales"));
		model.addAttribute("ocaTipoEntrega", session.getAttribute("ocaTipoEntrega"));
		model.addAttribute("ocaSucursalId", session.getAttribute("ocaSucursalId"));
		model.addAttribute("ocaSucursalSeleccionada", obtenerSucursalSeleccionada(session));
		model.addAttribute("checkoutCliente", session.getAttribute("checkoutCliente"));
		model.addAttribute("checkoutDireccion", session.getAttribute("checkoutDireccion"));
		BigDecimal costoEnvio = resolverCostoEnvio(resolverMetodoEntrega(null, session), session);
		model.addAttribute("costoEnvioCheckout", costoEnvio);
		model.addAttribute("totalCheckout", dinero(carrito.subtotal().add(costoEnvio)));
		return "finalizar-compra";
	}

	@PostMapping("/checkout/pago")
	public String prepararPago(
			@RequestParam(required = false) String metodoEnvio,
			@RequestParam(required = false) String ocaTipoEntrega,
			@RequestParam(required = false) String ocaSucursalId,
			Cliente cliente,
			DireccionEnvio direccionEnvio,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de pagar.");
			return "redirect:/carrito";
		}
		String metodoEntrega = normalizarMetodoEntrega(metodoEnvio);
		if ("OCA".equals(metodoEntrega)) {
			guardarSeleccionOca(session, ocaTipoEntrega, ocaSucursalId);
		}
		if ("OCA".equals(metodoEntrega) && !cotizacionOcaValida(session)) {
			redirectAttributes.addFlashAttribute("mensaje", "Cotizá el envío con OCA antes de continuar.");
			return "redirect:/checkout/datos";
		}
		if ("OCA".equals(metodoEntrega) && OCA_SUCURSAL.equals(resolverTipoEntregaOca(null, session)) && obtenerSucursalSeleccionada(session) == null) {
			redirectAttributes.addFlashAttribute("mensaje", "Elegí una sucursal OCA para retirar el envío.");
			return "redirect:/checkout/datos";
		}
		session.setAttribute("envioSeleccionado", metodoEntrega);
		session.setAttribute("checkoutCliente", cliente);
		session.setAttribute("checkoutDireccion", completarDireccion(direccionEnvio, session));
		return "redirect:/checkout/pago";
	}

	@GetMapping("/checkout/pago")
	public String pago(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de pagar.");
			return "redirect:/carrito";
		}
		model.addAttribute("pasoCheckout", 3);
		model.addAttribute("envioSeleccionado", session.getAttribute("envioSeleccionado"));
		model.addAttribute("codigoPostalDestino", session.getAttribute("codigoPostalDestino"));
		model.addAttribute("cotizacionOca", session.getAttribute("cotizacionOca"));
		model.addAttribute("cotizacionOcaDomicilio", session.getAttribute("cotizacionOcaDomicilio"));
		model.addAttribute("cotizacionOcaSucursal", session.getAttribute("cotizacionOcaSucursal"));
		model.addAttribute("ocaSucursales", session.getAttribute("ocaSucursales"));
		model.addAttribute("ocaTipoEntrega", session.getAttribute("ocaTipoEntrega"));
		model.addAttribute("ocaSucursalId", session.getAttribute("ocaSucursalId"));
		model.addAttribute("ocaSucursalSeleccionada", obtenerSucursalSeleccionada(session));
		model.addAttribute("checkoutCliente", session.getAttribute("checkoutCliente"));
		model.addAttribute("checkoutDireccion", session.getAttribute("checkoutDireccion"));
		BigDecimal costoEnvio = resolverCostoEnvio(resolverMetodoEntrega(null, session), session);
		model.addAttribute("costoEnvioCheckout", costoEnvio);
		model.addAttribute("totalCheckout", dinero(carrito.subtotal().add(costoEnvio)));
		return "finalizar-compra";
	}

	@PostMapping("/checkout/envio/cotizar")
	public String cotizarOca(
			@RequestParam(required = false) String codigoPostal,
			Cliente cliente,
			DireccionEnvio direccionEnvio,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de cotizar el envío.");
			return "redirect:/carrito";
		}
		DireccionEnvio direccion = completarDireccion(direccionEnvio, session);
		if (StringUtils.hasText(codigoPostal)) {
			direccion.setCodigoPostal(codigoPostal);
		}
		if (!StringUtils.hasText(direccion.getCodigoPostal()) || "0000".equals(direccion.getCodigoPostal())) {
			redirectAttributes.addFlashAttribute("mensaje", "Ingresá el código postal para cotizar OCA.");
			return "redirect:/checkout/datos";
		}
		session.setAttribute("checkoutCliente", cliente);
		session.setAttribute("checkoutDireccion", direccion);
		Pedido pedido = crearPedidoTemporalParaCotizar(carrito, direccion);
		List<OcaSucursalDTO> sucursales = ocaService.obtenerSucursales(direccion.getCodigoPostal());
		OcaCotizacionResponseDTO cotizacionDomicilio = ocaService.cotizarDomicilio(pedido);
		OcaCotizacionResponseDTO cotizacionSucursal = sucursales.isEmpty()
				? null
				: ocaService.cotizarSucursal(pedido);
		String tipoEntrega = sucursales.isEmpty() || cotizacionSucursal == null || !cotizacionSucursal.cotizada()
				? OCA_DOMICILIO
				: OCA_SUCURSAL;
		session.setAttribute("envioSeleccionado", "OCA");
		session.setAttribute("codigoPostalDestino", direccion.getCodigoPostal());
		session.setAttribute("ocaSucursales", sucursales);
		session.setAttribute("cotizacionOcaDomicilio", cotizacionDomicilio);
		session.setAttribute("cotizacionOcaSucursal", cotizacionSucursal);
		session.setAttribute("ocaTipoEntrega", tipoEntrega);
		session.setAttribute("ocaSucursalId", sucursales.isEmpty() ? null : sucursales.get(0).idCentroImposicion());
		actualizarCotizacionSeleccionada(session);
		if (!cotizacionOcaValida(session)) {
			OcaCotizacionResponseDTO cotizacion = cotizacionOcaSeleccionada(session);
			redirectAttributes.addFlashAttribute("mensaje", cotizacion == null ? "OCA no devolvió una tarifa para ese código postal." : cotizacion.mensaje());
		}
		return "redirect:/checkout/datos";
	}

	@PostMapping("/checkout/pagar")
	public String pagar(
			Cliente cliente,
			DireccionEnvio direccionEnvio,
			@RequestParam(required = false) String metodoEnvio,
			@RequestParam(required = false) String ocaTipoEntrega,
			@RequestParam(required = false) String ocaSucursalId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de pagar.");
			return "redirect:/carrito";
		}
		cliente = resolverCliente(cliente, session);
		direccionEnvio = resolverDireccion(direccionEnvio, session);
		String metodoEntrega = resolverMetodoEntrega(metodoEnvio, session);
		if ("OCA".equals(metodoEntrega)) {
			guardarSeleccionOca(session, ocaTipoEntrega, ocaSucursalId);
		}
		if ("OCA".equals(metodoEntrega) && !cotizacionOcaValida(session)) {
			redirectAttributes.addFlashAttribute("mensaje", "Cotizá el envío con OCA antes de pagar.");
			return "redirect:/checkout/datos";
		}
		if ("OCA".equals(metodoEntrega) && OCA_SUCURSAL.equals(resolverTipoEntregaOca(null, session)) && obtenerSucursalSeleccionada(session) == null) {
			redirectAttributes.addFlashAttribute("mensaje", "Elegí una sucursal OCA para retirar el envío.");
			return "redirect:/checkout/datos";
		}
		BigDecimal costoEnvio = resolverCostoEnvio(metodoEntrega, session);
		session.setAttribute("envioSeleccionado", metodoEntrega);
		Pedido pedido;
		try {
			pedido = pedidoService.crearPedidoWeb(cliente, direccionEnvio, carrito, metodoEntrega, costoEnvio);
		} catch (IllegalStateException | IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("mensaje", ex.getMessage());
			return "redirect:/carrito";
		}
		if ("OCA".equals(metodoEntrega)) {
			ocaService.guardarCotizacion(
					pedido,
					cotizacionOcaSeleccionada(session),
					resolverTipoEntregaOca(null, session),
					obtenerSucursalSeleccionada(session));
		}
		MercadoPagoPreferenceResponseDTO preference;
		try {
			preference = mercadoPagoService.crearPreferencia(pedido);
		} catch (RuntimeException ex) {
			pedidoService.cancelarPedido(pedido.getId(), "No se pudo crear la preferencia de Mercado Pago");
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo iniciar Mercado Pago. Intentá nuevamente.");
			return "redirect:/carrito";
		}
		String checkoutUrl = resolverCheckoutUrl(preference);
		if (checkoutUrl != null && !checkoutUrl.isBlank()) {
			return "redirect:" + checkoutUrl;
		}
		redirectAttributes.addFlashAttribute("pedido", pedido);
		redirectAttributes.addFlashAttribute("mensaje", "Pedido creado. Falta configurar Mercado Pago para redirigir al pago.");
		return "redirect:/finalizar-compra";
	}

	@GetMapping({ "/checkout/mercadopago/success", "/checkout/mercadopago/failure", "/checkout/mercadopago/pending" })
	public String retornoMercadoPago(
			@RequestParam(required = false, name = "external_reference") String externalReference,
			@RequestParam(required = false, name = "payment_id") String paymentId,
			@RequestParam(required = false) String status,
			Model model,
			HttpSession session,
			HttpServletRequest request) {
		String codigoCompra = null;
		String estado = resolverEstadoRetorno(status, request.getRequestURI());
		MercadoPagoPaymentDataDTO pagoMercadoPago = obtenerPagoMercadoPago(paymentId);
		if (pagoMercadoPago != null && StringUtils.hasText(pagoMercadoPago.externalReference())) {
			estado = pagoMercadoPago.status();
			pedidoService.actualizarPagoMercadoPago(pagoMercadoPago);
			externalReference = pagoMercadoPago.externalReference();
		}
		model.addAttribute("titulo", "Estado del pago");
		model.addAttribute("mensaje", "Mercado Pago informó estado: " + (estado == null ? "sin estado" : estado));
		if (externalReference != null && !externalReference.isBlank()) {
			codigoCompra = pagoRepository.findByExternalReference(externalReference)
					.map(pago -> pago.getPedido().getCodigoCompra())
					.orElse(null);
			if (codigoCompra != null && !codigoCompra.isBlank()) {
				model.addAttribute("codigoCompra", codigoCompra);
			}
		}
		model.addAttribute("whatsappContactoLink", construirWhatsappContactoLink(codigoCompra));
		limpiarCheckout(session);
		model.addAttribute("carrito", new CarritoDTO());
		return "checkout-resultado";
	}

	private Pedido crearPedidoTemporalParaCotizar(CarritoDTO carrito, DireccionEnvio direccion) {
		Pedido pedido = new Pedido();
		pedido.setDireccionEnvio(direccion);
		carrito.items().forEach(itemCarrito -> {
			Producto producto = productoRepository.findById(itemCarrito.productoId())
					.orElseThrow(() -> new IllegalArgumentException("Producto inexistente: " + itemCarrito.productoId()));
			PedidoItem item = new PedidoItem();
			item.setProducto(producto);
			item.setCantidad(itemCarrito.cantidad());
			item.setPrecioUnitarioSnapshot(producto.getPrecio());
			item.setNombreSnapshot(producto.getNombre());
			item.calcularSubtotal();
			pedido.agregarItem(item);
		});
		return pedido;
	}

	private String resolverMetodoEntrega(String metodoEnvio, HttpSession session) {
		if (metodoEnvio != null && !metodoEnvio.isBlank()) {
			return normalizarMetodoEntrega(metodoEnvio);
		}
		Object sessionMetodo = session.getAttribute("envioSeleccionado");
		return sessionMetodo instanceof String value && !value.isBlank() ? normalizarMetodoEntrega(value) : "SUCURSAL";
	}

	private String normalizarMetodoEntrega(String metodoEnvio) {
		return switch (metodoEnvio == null ? "" : metodoEnvio) {
			case "OCA" -> "OCA";
			case "VENDEDOR" -> "VENDEDOR";
			default -> "SUCURSAL";
		};
	}

	private BigDecimal resolverCostoEnvio(String metodoEntrega, HttpSession session) {
		if ("OCA".equals(metodoEntrega)) {
			OcaCotizacionResponseDTO ocaCotizacion = cotizacionOcaSeleccionada(session);
			if (ocaCotizacion != null && ocaCotizacion.cotizada()) {
				return dinero(ocaCotizacion.costo());
			}
		}
		return dinero(BigDecimal.ZERO);
	}

	private BigDecimal dinero(BigDecimal valor) {
		return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
	}

	private boolean cotizacionOcaValida(HttpSession session) {
		OcaCotizacionResponseDTO cotizacion = cotizacionOcaSeleccionada(session);
		return cotizacion != null && cotizacion.cotizada();
	}

	private OcaCotizacionResponseDTO cotizacionOcaSeleccionada(HttpSession session) {
		String tipoEntrega = resolverTipoEntregaOca(null, session);
		Object cotizacion = OCA_SUCURSAL.equals(tipoEntrega)
				? session.getAttribute("cotizacionOcaSucursal")
				: session.getAttribute("cotizacionOcaDomicilio");
		if (cotizacion instanceof OcaCotizacionResponseDTO ocaCotizacion) {
			return ocaCotizacion;
		}
		Object fallback = session.getAttribute("cotizacionOca");
		return fallback instanceof OcaCotizacionResponseDTO ocaCotizacion ? ocaCotizacion : null;
	}

	private void actualizarCotizacionSeleccionada(HttpSession session) {
		OcaCotizacionResponseDTO cotizacion = cotizacionOcaSeleccionada(session);
		if (cotizacion != null) {
			session.setAttribute("cotizacionOca", cotizacion);
		}
	}

	private void guardarSeleccionOca(HttpSession session, String tipoEntrega, String sucursalId) {
		String tipoNormalizado = normalizarTipoEntregaOca(StringUtils.hasText(tipoEntrega) ? tipoEntrega : resolverTipoEntregaOca(null, session));
		session.setAttribute("ocaTipoEntrega", tipoNormalizado);
		if (OCA_SUCURSAL.equals(tipoNormalizado)) {
			String idSeleccionado = StringUtils.hasText(sucursalId) ? sucursalId : (String) session.getAttribute("ocaSucursalId");
			if (!StringUtils.hasText(idSeleccionado)) {
				List<OcaSucursalDTO> sucursales = sucursalesOca(session);
				idSeleccionado = sucursales.isEmpty() ? null : sucursales.get(0).idCentroImposicion();
			}
			session.setAttribute("ocaSucursalId", idSeleccionado);
		}
		actualizarCotizacionSeleccionada(session);
	}

	private String resolverTipoEntregaOca(String tipoEntrega, HttpSession session) {
		if (StringUtils.hasText(tipoEntrega)) {
			return normalizarTipoEntregaOca(tipoEntrega);
		}
		Object sessionTipo = session.getAttribute("ocaTipoEntrega");
		return sessionTipo instanceof String value && StringUtils.hasText(value) ? normalizarTipoEntregaOca(value) : OCA_DOMICILIO;
	}

	private String normalizarTipoEntregaOca(String tipoEntrega) {
		return OCA_SUCURSAL.equals(tipoEntrega) ? OCA_SUCURSAL : OCA_DOMICILIO;
	}

	private OcaSucursalDTO obtenerSucursalSeleccionada(HttpSession session) {
		Object selected = session.getAttribute("ocaSucursalId");
		if (!(selected instanceof String sucursalId) || !StringUtils.hasText(sucursalId)) {
			return null;
		}
		return sucursalesOca(session).stream()
				.filter(sucursal -> sucursalId.equals(sucursal.idCentroImposicion()))
				.findFirst()
				.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private List<OcaSucursalDTO> sucursalesOca(HttpSession session) {
		Object sucursales = session.getAttribute("ocaSucursales");
		if (sucursales instanceof List<?> lista) {
			return (List<OcaSucursalDTO>) lista;
		}
		return List.of();
	}

	private String resolverCheckoutUrl(MercadoPagoPreferenceResponseDTO preference) {
		if (mercadoPagoProperties.isSandbox()
				&& preference.sandboxInitPoint() != null
				&& !preference.sandboxInitPoint().isBlank()) {
			return preference.sandboxInitPoint();
		}
		return preference.initPoint();
	}

	private String construirWhatsappContactoLink(String codigoCompra) {
		String mensaje = codigoCompra == null || codigoCompra.isBlank()
				? "Hola! compré por la página, quiero preguntar..."
				: "Hola! compré por la página, mi codigo de compra es " + codigoCompra + ", quiero preguntar...";
		return configuracionTiendaService.obtener().getWhatsappLink()
				+ "?text="
				+ URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
	}

	private void limpiarCheckout(HttpSession session) {
		session.removeAttribute("carrito");
		session.removeAttribute("envioSeleccionado");
		session.removeAttribute("codigoPostalDestino");
		session.removeAttribute("cotizacionOca");
		session.removeAttribute("cotizacionOcaDomicilio");
		session.removeAttribute("cotizacionOcaSucursal");
		session.removeAttribute("ocaSucursales");
		session.removeAttribute("ocaTipoEntrega");
		session.removeAttribute("ocaSucursalId");
		session.removeAttribute("checkoutCliente");
		session.removeAttribute("checkoutDireccion");
	}

	private DireccionEnvio completarDireccion(DireccionEnvio direccion, HttpSession session) {
		DireccionEnvio destino = direccion == null ? new DireccionEnvio() : direccion;
		if (destino.getCalle() == null || destino.getCalle().isBlank()) {
			destino.setCalle("Sin definir");
		}
		if (destino.getNumero() == null || destino.getNumero().isBlank()) {
			destino.setNumero("0");
		}
		if (destino.getCiudad() == null || destino.getCiudad().isBlank()) {
			destino.setCiudad("Sin definir");
		}
		if (destino.getProvincia() == null || destino.getProvincia().isBlank()) {
			destino.setProvincia("Sin definir");
		}
		if (destino.getCodigoPostal() == null || destino.getCodigoPostal().isBlank()) {
			Object codigoPostal = session.getAttribute("codigoPostalDestino");
			destino.setCodigoPostal(codigoPostal instanceof String cp && !cp.isBlank() ? cp : "0000");
		}
		if (destino.getPais() == null || destino.getPais().isBlank()) {
			destino.setPais("AR");
		}
		return destino;
	}

	private Cliente resolverCliente(Cliente cliente, HttpSession session) {
		if (cliente != null && cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
			return cliente;
		}
		Object sessionCliente = session.getAttribute("checkoutCliente");
		if (sessionCliente instanceof Cliente clienteGuardado) {
			return clienteGuardado;
		}
		return cliente;
	}

	private DireccionEnvio resolverDireccion(DireccionEnvio direccion, HttpSession session) {
		if (direccion != null && direccion.getCodigoPostal() != null && !direccion.getCodigoPostal().isBlank()) {
			return completarDireccion(direccion, session);
		}
		Object sessionDireccion = session.getAttribute("checkoutDireccion");
		if (sessionDireccion instanceof DireccionEnvio direccionGuardada) {
			return completarDireccion(direccionGuardada, session);
		}
		return completarDireccion(direccion, session);
	}

	private String resolverEstadoRetorno(String status, String requestUri) {
		if (status != null && !status.isBlank()) {
			return status;
		}
		if (requestUri != null && requestUri.endsWith("/success")) {
			return "approved";
		}
		if (requestUri != null && requestUri.endsWith("/failure")) {
			return "rejected";
		}
		if (requestUri != null && requestUri.endsWith("/pending")) {
			return "pending";
		}
		return status;
	}

	private MercadoPagoPaymentDataDTO obtenerPagoMercadoPago(String paymentId) {
		if (!StringUtils.hasText(paymentId)) {
			return null;
		}
		try {
			JsonNode payment = RestClient.create(mercadoPagoProperties.getApiUrl())
					.get()
					.uri("/v1/payments/{paymentId}", paymentId)
					.header("Authorization", "Bearer " + mercadoPagoProperties.getAccessToken())
					.retrieve()
					.body(JsonNode.class);
			return new MercadoPagoPaymentDataDTO(
					textOrNull(payment.path("external_reference")),
					paymentId,
					textOrNull(payment.path("status")),
					textOrNull(payment.path("status_detail")),
					textOrNull(payment.path("payment_method_id")),
					textOrNull(payment.path("payment_type_id")),
					decimalOrNull(payment.path("transaction_amount")));
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private BigDecimal decimalOrNull(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		try {
			return new BigDecimal(node.asText());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private String textOrNull(JsonNode node) {
		return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
	}

}
