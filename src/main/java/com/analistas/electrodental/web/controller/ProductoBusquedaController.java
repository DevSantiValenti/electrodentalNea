package com.analistas.electrodental.web.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.ProductoBusquedaDTO;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IProductoService;

@RestController
public class ProductoBusquedaController {

	private final IProductoService productoService;
	private final IConfiguracionTiendaService configuracionTiendaService;

	public ProductoBusquedaController(
			IProductoService productoService,
			IConfiguracionTiendaService configuracionTiendaService) {
		this.productoService = productoService;
		this.configuracionTiendaService = configuracionTiendaService;
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
				: configuracionTiendaService.obtener().getLogoUrl();
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
