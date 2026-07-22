package com.analistas.electrodental.web.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.ICarritoService;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IDescuentoService;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@ControllerAdvice
public class GlobalModelControllerAdvice {

	private final ICarritoService carritoService;
	private final ICategoriaService categoriaService;
	private final IConfiguracionTiendaService configuracionTiendaService;
	private final IProductoRepository productoRepository;
	private final IDescuentoService descuentoService;

	public GlobalModelControllerAdvice(
			ICarritoService carritoService,
			ICategoriaService categoriaService,
			IConfiguracionTiendaService configuracionTiendaService,
			IProductoRepository productoRepository,
			IDescuentoService descuentoService) {
		this.carritoService = carritoService;
		this.categoriaService = categoriaService;
		this.configuracionTiendaService = configuracionTiendaService;
		this.productoRepository = productoRepository;
		this.descuentoService = descuentoService;
	}

	@ModelAttribute("carrito")
	public CarritoDTO carrito(HttpSession session, HttpServletRequest request) {
		if (esVistaError(request)) {
			return new CarritoDTO();
		}
		Object carrito = session.getAttribute("carrito");
		if (carrito instanceof CarritoDTO carritoDTO) {
			CarritoDTO actualizado = descuentoService.recalcular(carritoDTO);
			if (actualizado != carritoDTO) {
				session.setAttribute("carrito", actualizado);
			}
			return actualizado;
		}
		CarritoDTO nuevo = carritoService.nuevoCarrito();
		session.setAttribute("carrito", nuevo);
		return nuevo;
	}

	@ModelAttribute("descuentoTransferenciaCheckout")
	public BigDecimal descuentoTransferenciaCheckout(HttpSession session, HttpServletRequest request) {
		return carrito(session, request).descuentoTransferencia();
	}

	@ModelAttribute("totalTransferenciaCheckout")
	public BigDecimal totalTransferenciaCheckout(HttpSession session, HttpServletRequest request) {
		return carrito(session, request).totalTransferencia();
	}

	@ModelAttribute("categoriasNav")
	public List<Categoria> categoriasNav(HttpServletRequest request) {
		if (esVistaError(request)) {
			return List.of();
		}
		return categoriaService.listarActivas();
	}

	@ModelAttribute("configuracionTienda")
	public ConfiguracionTienda configuracionTienda(HttpServletRequest request) {
		if (esVistaError(request)) {
			return new ConfiguracionTienda();
		}
		return configuracionTiendaService.obtener();
	}

	@ModelAttribute("ocaDisponible")
	public boolean ocaDisponible(HttpSession session, HttpServletRequest request) {
		if (esVistaError(request)) {
			return true;
		}
		CarritoDTO carrito = carrito(session, request);
		if (carrito == null || carrito.items().isEmpty()) {
			return true;
		}
		boolean bloqueadoEnCarrito = carrito.items().stream()
				.anyMatch(item -> Boolean.TRUE.equals(item.envioOcaDesactivado()));
		if (bloqueadoEnCarrito) {
			return false;
		}
		return carrito.items().stream()
				.map(item -> productoRepository.findById(item.productoId()))
				.noneMatch(producto -> producto
						.map(Producto::getEnvioOcaDesactivado)
						.map(Boolean.TRUE::equals)
						.orElse(false));
	}

	private boolean esVistaError(HttpServletRequest request) {
		return request != null
				&& ("/error".equals(request.getRequestURI())
						|| request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) != null);
	}
}
