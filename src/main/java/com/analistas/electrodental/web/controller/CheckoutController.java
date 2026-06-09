package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
import com.analistas.electrodental.model.domain.dto.MercadoPagoPreferenceResponseDTO;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IMercadoPagoService;
import com.analistas.electrodental.model.service.IPedidoService;
import com.analistas.electrodental.web.config.MercadoPagoProperties;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.JsonNode;

@Controller
public class CheckoutController {

	private final IPedidoService pedidoService;
	private final IMercadoPagoService mercadoPagoService;
	private final IProductoRepository productoRepository;
	private final IPagoRepository pagoRepository;
	private final MercadoPagoProperties mercadoPagoProperties;
	private final IConfiguracionTiendaService configuracionTiendaService;

	public CheckoutController(
			IPedidoService pedidoService,
			IMercadoPagoService mercadoPagoService,
			IProductoRepository productoRepository,
			IPagoRepository pagoRepository,
			MercadoPagoProperties mercadoPagoProperties,
			IConfiguracionTiendaService configuracionTiendaService) {
		this.pedidoService = pedidoService;
		this.mercadoPagoService = mercadoPagoService;
		this.productoRepository = productoRepository;
		this.pagoRepository = pagoRepository;
		this.mercadoPagoProperties = mercadoPagoProperties;
		this.configuracionTiendaService = configuracionTiendaService;
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
		model.addAttribute("cotizacionAndreani", session.getAttribute("cotizacionAndreani"));
		model.addAttribute("checkoutCliente", session.getAttribute("checkoutCliente"));
		model.addAttribute("checkoutDireccion", session.getAttribute("checkoutDireccion"));
		return "finalizar-compra";
	}

	@PostMapping("/checkout/pago")
	public String prepararPago(
			@RequestParam(required = false) String metodoEnvio,
			Cliente cliente,
			DireccionEnvio direccionEnvio,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de pagar.");
			return "redirect:/carrito";
		}
		session.setAttribute("envioSeleccionado", normalizarMetodoEntrega(metodoEnvio));
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
		model.addAttribute("cotizacionAndreani", session.getAttribute("cotizacionAndreani"));
		model.addAttribute("checkoutCliente", session.getAttribute("checkoutCliente"));
		model.addAttribute("checkoutDireccion", session.getAttribute("checkoutDireccion"));
		return "finalizar-compra";
	}

	@PostMapping("/checkout/envio/cotizar")
	public String cotizarAndreani(
			@RequestParam String codigoPostalDestino,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		// Andreani queda pausado temporalmente. Para reactivarlo, restaurar la cotizacion con andreaniService.cotizar.
		redirectAttributes.addFlashAttribute("mensaje", "La cotización con Andreani está desactivada temporalmente.");
		session.removeAttribute("cotizacionAndreani");
		session.setAttribute("envioSeleccionado", "SUCURSAL");
		session.setAttribute("codigoPostalDestino", codigoPostalDestino);
		return "redirect:/checkout/datos";
		/*
		CarritoDTO carrito = (CarritoDTO) session.getAttribute("carrito");
		if (carrito == null || carrito.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "Agrega productos al carrito antes de cotizar el envío.");
			return "redirect:/carrito";
		}
		Pedido pedido = crearPedidoTemporalParaCotizar(carrito, codigoPostalDestino);
		AndreaniCotizacionResponseDTO cotizacion = andreaniService.cotizar(pedido);
		session.setAttribute("envioSeleccionado", "ANDREANI");
		session.setAttribute("codigoPostalDestino", codigoPostalDestino);
		session.setAttribute("cotizacionAndreani", cotizacion);
		return "redirect:/checkout/datos";
		*/
	}

	@PostMapping("/checkout/pagar")
	public String pagar(
			Cliente cliente,
			DireccionEnvio direccionEnvio,
			@RequestParam(required = false) String metodoEnvio,
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
		BigDecimal costoEnvio = resolverCostoEnvio(metodoEntrega, session);
		session.setAttribute("envioSeleccionado", metodoEntrega);
		Pedido pedido;
		try {
			pedido = pedidoService.crearPedidoWeb(cliente, direccionEnvio, carrito, metodoEntrega, costoEnvio);
		} catch (IllegalStateException | IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("mensaje", ex.getMessage());
			return "redirect:/carrito";
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
		PagoMercadoPago pagoMercadoPago = obtenerPagoMercadoPago(paymentId);
		if (pagoMercadoPago != null && StringUtils.hasText(pagoMercadoPago.externalReference())) {
			estado = pagoMercadoPago.status();
			pedidoService.actualizarPagoMercadoPago(
					pagoMercadoPago.externalReference(),
					pagoMercadoPago.paymentId(),
					pagoMercadoPago.status());
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

	private Pedido crearPedidoTemporalParaCotizar(CarritoDTO carrito, String codigoPostalDestino) {
		DireccionEnvio direccion = new DireccionEnvio();
		direccion.setCodigoPostal(codigoPostalDestino);
		direccion.setCiudad("Sin definir");

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
		return "VENDEDOR".equals(metodoEnvio) ? "VENDEDOR" : "SUCURSAL";
	}

	private BigDecimal resolverCostoEnvio(String metodoEntrega, HttpSession session) {
		// Andreani queda pausado temporalmente: no se suma costo de envio cotizado.
		return BigDecimal.ZERO;
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
		session.removeAttribute("cotizacionAndreani");
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

	private PagoMercadoPago obtenerPagoMercadoPago(String paymentId) {
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
			String externalReference = textOrNull(payment.path("external_reference"));
			String status = textOrNull(payment.path("status"));
			return new PagoMercadoPago(paymentId, externalReference, status);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private String textOrNull(JsonNode node) {
		return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
	}

	private record PagoMercadoPago(String paymentId, String externalReference, String status) {
	}
}
