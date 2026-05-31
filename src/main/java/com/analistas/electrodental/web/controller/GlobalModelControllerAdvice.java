package com.analistas.electrodental.web.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.ICarritoService;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@ControllerAdvice
public class GlobalModelControllerAdvice {

	private final ICarritoService carritoService;
	private final ICategoriaService categoriaService;
	private final IConfiguracionTiendaService configuracionTiendaService;

	public GlobalModelControllerAdvice(
			ICarritoService carritoService,
			ICategoriaService categoriaService,
			IConfiguracionTiendaService configuracionTiendaService) {
		this.carritoService = carritoService;
		this.categoriaService = categoriaService;
		this.configuracionTiendaService = configuracionTiendaService;
	}

	@ModelAttribute("carrito")
	public CarritoDTO carrito(HttpSession session) {
		Object carrito = session.getAttribute("carrito");
		if (carrito instanceof CarritoDTO carritoDTO) {
			return carritoDTO;
		}
		CarritoDTO nuevo = carritoService.nuevoCarrito();
		session.setAttribute("carrito", nuevo);
		return nuevo;
	}

	@ModelAttribute("categoriasNav")
	public List<Categoria> categoriasNav() {
		return categoriaService.listarActivas();
	}

	@ModelAttribute("configuracionTienda")
	public ConfiguracionTienda configuracionTienda() {
		return configuracionTiendaService.obtener();
	}
}
