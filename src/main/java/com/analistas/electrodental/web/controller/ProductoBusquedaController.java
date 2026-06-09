package com.analistas.electrodental.web.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.ProductoBusquedaDTO;
import com.analistas.electrodental.model.service.IProductoService;

@RestController
public class ProductoBusquedaController {

	private static final String IMAGEN_DEFAULT = "/img/electrodentallarge.png";

	private final IProductoService productoService;

	public ProductoBusquedaController(IProductoService productoService) {
		this.productoService = productoService;
	}

	@GetMapping("/api/productos/buscar")
	public List<ProductoBusquedaDTO> buscar(@RequestParam(name = "q", required = false) String termino) {
		if (!StringUtils.hasText(termino) || termino.trim().length() < 2) {
			return List.of();
		}
		return productoService.buscarSugerencias(termino, 8)
				.stream()
				.map(this::toDto)
				.toList();
	}

	private ProductoBusquedaDTO toDto(Producto producto) {
		String imagen = StringUtils.hasText(producto.getImagenPrincipal())
				? producto.getImagenPrincipal()
				: IMAGEN_DEFAULT;
		return new ProductoBusquedaDTO(
				producto.getId(),
				producto.getNombre(),
				producto.getMarca(),
				imagen,
				"/productos/" + producto.getSlug(),
				producto.getPrecio(),
				producto.getStockFisico(),
				producto.getStockWeb());
	}
}
