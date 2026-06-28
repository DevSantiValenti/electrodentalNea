package com.analistas.electrodental.web.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.service.ICarritoService;
import com.analistas.electrodental.model.service.IDescuentoService;
import com.analistas.electrodental.model.service.IProductoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CarritoController {

	private final ICarritoService carritoService;
	private final IProductoService productoService;
	private final IDescuentoService descuentoService;

	public CarritoController(ICarritoService carritoService, IProductoService productoService, IDescuentoService descuentoService) {
		this.carritoService = carritoService;
		this.productoService = productoService;
		this.descuentoService = descuentoService;
	}

	@PostMapping("/carrito/agregar")
	public Object agregar(
			HttpSession session,
			HttpServletRequest request,
			@RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
			@RequestParam Long productoId,
			@RequestParam(defaultValue = "1") Integer cantidad,
			Model model) {
		CarritoDTO carrito = obtenerCarrito(session);
		boolean ajax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
		return productoService.buscarPorId(productoId)
				.<Object>map(producto -> {
					if (!producto.disponibleParaCompraWeb()) {
						String mensaje = producto.permiteCompraWeb()
								? "Este producto no tiene stock disponible."
								: "Este producto está disponible solo para consulta.";
						return ajax
								? ResponseEntity.badRequest().body(Map.of(
										"ok", false,
										"message", mensaje,
										"cartCount", carrito.cantidadTotal()))
								: "redirect:" + resolverRetorno(request);
					}
					int cantidadAnterior = carrito.cantidadTotal();
					CarritoDTO actualizado = descuentoService.recalcular(carritoService.agregarProducto(carrito, producto, cantidad));
					session.setAttribute("carrito", actualizado);
					model.addAttribute("carrito", actualizado);
					if (ajax) {
						String mensaje = actualizado.cantidadTotal() > cantidadAnterior
								? producto.getNombre() + " agregado al carrito"
								: "No hay más stock disponible para " + producto.getNombre();
						return ResponseEntity.ok(Map.of(
								"ok", true,
								"message", mensaje,
								"cartCount", actualizado.cantidadTotal()));
					}
					return "redirect:" + resolverRetorno(request);
				})
				.orElseGet(() -> ajax
						? ResponseEntity.badRequest().body(Map.of(
								"ok", false,
								"message", "Producto no encontrado",
								"cartCount", carrito.cantidadTotal()))
						: "redirect:/catalogo");
	}

	@PostMapping("/carrito/actualizar/{productoId}")
	public String actualizar(
			HttpSession session,
			@PathVariable Long productoId,
			@RequestParam Integer cantidad,
			Model model,
			RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = obtenerCarrito(session);
		return productoService.buscarPorId(productoId)
				.map(producto -> {
					CarritoDTO actualizado = descuentoService.recalcular(carritoService.actualizarCantidad(carrito, producto, cantidad));
					avisarSiSeQuitoDescuento(carrito, actualizado, redirectAttributes);
					model.addAttribute("carrito", actualizado);
					session.setAttribute("carrito", actualizado);
					return "redirect:/carrito";
				})
				.orElse("redirect:/carrito");
	}

	@PostMapping("/carrito/quitar/{productoId}")
	public String quitar(
			HttpSession session,
			@PathVariable Long productoId,
			Model model,
			RedirectAttributes redirectAttributes) {
		CarritoDTO carrito = obtenerCarrito(session);
		CarritoDTO actualizado = descuentoService.recalcular(carritoService.quitarProducto(carrito, productoId));
		avisarSiSeQuitoDescuento(carrito, actualizado, redirectAttributes);
		session.setAttribute("carrito", actualizado);
		model.addAttribute("carrito", actualizado);
		return "redirect:/carrito";
	}

	@PostMapping("/carrito/descuento/aplicar")
	public String aplicarDescuento(
			HttpSession session,
			@RequestParam(required = false) String codigo,
			RedirectAttributes redirectAttributes) {
		var resultado = descuentoService.aplicar(obtenerCarrito(session), codigo);
		session.setAttribute("carrito", resultado.carrito());
		redirectAttributes.addFlashAttribute(resultado.valido() ? "mensajeOk" : "mensaje", resultado.mensaje());
		return "redirect:/carrito";
	}

	@PostMapping("/carrito/descuento/quitar")
	public String quitarDescuento(HttpSession session, RedirectAttributes redirectAttributes) {
		CarritoDTO actualizado = obtenerCarrito(session).sinDescuento();
		session.setAttribute("carrito", actualizado);
		redirectAttributes.addFlashAttribute("mensajeOk", "Descuento quitado del carrito.");
		return "redirect:/carrito";
	}

	private CarritoDTO obtenerCarrito(HttpSession session) {
		Object carrito = session.getAttribute("carrito");
		return carrito instanceof CarritoDTO carritoDTO ? carritoDTO : carritoService.nuevoCarrito();
	}

	private String resolverRetorno(HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		if (referer == null || referer.isBlank()) {
			return "/catalogo";
		}
		return referer;
	}

	private void avisarSiSeQuitoDescuento(CarritoDTO anterior, CarritoDTO actualizado, RedirectAttributes redirectAttributes) {
		if (anterior != null && anterior.tieneDescuento() && (actualizado == null || !actualizado.tieneDescuento())) {
			redirectAttributes.addFlashAttribute("mensaje", "El descuento se quitó porque el carrito ya no cumple sus condiciones.");
		}
	}
}
