package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.IProductoService;

@Controller
public class SiteController {

	private final IProductoService productoService;
	private final ICategoriaService categoriaService;

	public SiteController(
			IProductoService productoService,
			ICategoriaService categoriaService) {
		this.productoService = productoService;
		this.categoriaService = categoriaService;
	}

	@GetMapping({ "/", "/inicio" })
	public String inicio(Model model) {
		model.addAttribute("destacados", productoService.listarDestacados());
		model.addAttribute("ofertas", productoService.listarOfertas());
		model.addAttribute("bajoStock", productoService.listarBajoStock());
		model.addAttribute("seoDescription", "Electrodental NEA ofrece equipamiento odontologico, insumos dentales, repuestos y servicio tecnico para consultorios, clinicas y laboratorios.");
		return "home";
	}

	@GetMapping("/catalogo/categoria/{categoria}")
	public String catalogoCategoria(@PathVariable String categoria, Model model) {
		return catalogo(null, categoria, null, null, null, null, model);
	}

	@GetMapping("/catalogo/subcategoria/{subcategoria}")
	public String catalogoSubcategoria(@PathVariable String subcategoria, Model model) {
		return catalogo(null, null, subcategoria, null, null, null, model);
	}

	@GetMapping({ "/catalogo", "/productos" })
	public String catalogo(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String categoria,
			@RequestParam(required = false) String subcategoria,
			@RequestParam(required = false) String marca,
			@RequestParam(required = false) BigDecimal precioMin,
			@RequestParam(required = false) BigDecimal precioMax,
			Model model) {
		List<Producto> productos;
		if (subcategoria != null && !subcategoria.isBlank()) {
			productos = productoService.listarPorSubcategoria(subcategoria);
			categoriaService.buscarSubcategoriaPorSlug(subcategoria).ifPresent(subcategoriaSeleccionada -> {
				model.addAttribute("subcategoriaActual", subcategoriaSeleccionada);
				model.addAttribute("categoriaActual", subcategoriaSeleccionada.getCategoria());
				model.addAttribute("subcategorias", categoriaService.listarSubcategoriasActivasPorCategoria(subcategoriaSeleccionada.getCategoria().getId()));
			});
		} else if (categoria != null && !categoria.isBlank()) {
			productos = productoService.listarPorCategoria(categoria);
			categoriaService.buscarCategoriaPorSlug(categoria).ifPresent(categoriaSeleccionada -> {
				model.addAttribute("categoriaActual", categoriaSeleccionada);
				model.addAttribute("subcategorias", categoriaService.listarSubcategoriasActivasPorCategoria(categoriaSeleccionada.getId()));
			});
		} else {
			productos = productoService.listarActivos();
		}

		productos = aplicarFiltros(productos, q, marca, precioMin, precioMax);
		model.addAttribute("categorias", categoriaService.listarActivas());
		model.addAttribute("productos", productos);
		model.addAttribute("marcas", productoService.listarActivos().stream()
				.map(com.analistas.electrodental.model.domain.Producto::getMarca)
				.filter(marcaProducto -> marcaProducto != null && !marcaProducto.isBlank())
				.distinct()
				.sorted()
				.toList());
		model.addAttribute("busqueda", q);
		model.addAttribute("categoriaSeleccionada", categoria);
		model.addAttribute("subcategoriaSeleccionada", subcategoria);
		model.addAttribute("marcaSeleccionada", marca);
		model.addAttribute("precioMin", precioMin);
		model.addAttribute("precioMax", precioMax);
		Object categoriaActual = model.asMap().get("categoriaActual");
		String titulo = categoriaActual instanceof com.analistas.electrodental.model.domain.Categoria categoriaActualModel
				? "Catalogo de " + categoriaActualModel.getNombre()
				: "Catalogo de productos odontologicos";
		model.addAttribute("seoDescription", titulo + " en Electrodental NEA: equipamiento, insumos dentales, repuestos y productos profesionales para odontologia.");
		return "catalogo";
	}

	private List<Producto> aplicarFiltros(
			List<Producto> productos,
			String q,
			String marca,
			BigDecimal precioMin,
			BigDecimal precioMax) {
		return productos.stream()
				.filter(producto -> q == null || q.isBlank() || producto.getNombre().toLowerCase().contains(q.toLowerCase()))
				.filter(producto -> marca == null || marca.isBlank() || marca.equalsIgnoreCase(producto.getMarca()))
				.filter(producto -> precioMin == null || producto.getPrecio().compareTo(precioMin) >= 0)
				.filter(producto -> precioMax == null || producto.getPrecio().compareTo(precioMax) <= 0)
				.toList();
	}

	@GetMapping({ "/producto", "/producto/{slug}", "/productos/{slug}" })
	public String producto(@PathVariable(required = false) String slug, Model model) {
		if (slug != null) {
			productoService.buscarPorSlug(slug).ifPresent(producto -> {
				model.addAttribute("producto", producto);
				model.addAttribute("productoImagenes", obtenerImagenesProducto(producto));
				model.addAttribute("caracteristicasProducto", obtenerCaracteristicasProducto(producto));
				model.addAttribute("productosRelacionados", productoService.listarRelacionados(producto));
				model.addAttribute("seoDescription", descripcionProductoSeo(producto));
			});
		}
		return "producto";
	}

	@GetMapping({ "/carrito", "/checkout", "/finalizar-compra" })
	public String finalizarCompra(Model model) {
		model.addAttribute("pasoCheckout", 1);
		model.addAttribute("seoDescription", "Revisa tu carrito de Electrodental NEA y finaliza la compra de equipamiento e insumos odontologicos de forma segura.");
		return "finalizar-compra";
	}

	@GetMapping({ "/servicio-tecnico", "/denttech" })
	public String servicioTecnico() {
		return "servicio-tecnico";
	}

	@GetMapping("/ofertas")
	public String ofertas(Model model) {
		model.addAttribute("productos", productoService.listarOfertas());
		model.addAttribute("categorias", categoriaService.listarActivas());
		model.addAttribute("marcas", productoService.listarActivos().stream()
				.map(com.analistas.electrodental.model.domain.Producto::getMarca)
				.filter(marcaProducto -> marcaProducto != null && !marcaProducto.isBlank())
				.distinct()
				.sorted()
				.toList());
		model.addAttribute("seoDescription", "Ofertas de Electrodental NEA en equipamiento odontologico, insumos dentales y productos profesionales seleccionados.");
		return "catalogo";
	}

	// @GetMapping("/marcas")
	// public String marcas(Model model) {
	// 	model.addAttribute("titulo", "Marcas de confianza");
	// 	model.addAttribute("mensaje", "Trabajamos con marcas odontológicas profesionales. En la home podés ver el carrusel completo de logos.");
	// 	return "simple-page";
	// }

	@GetMapping("/contacto")
	public String contacto(Model model) {
		model.addAttribute("seoDescription", "Contacta a Electrodental NEA para asesoramiento comercial, presupuestos, insumos odontologicos y servicio tecnico dental.");
		return "contact";
	}

	private String descripcionProductoSeo(Producto producto) {
		String marca = producto.getMarca() == null || producto.getMarca().isBlank() ? "Electrodental NEA" : producto.getMarca();
		String descripcion = producto.getDescripcion() == null || producto.getDescripcion().isBlank()
				? "Producto odontologico profesional disponible en Electrodental NEA."
				: producto.getDescripcion().replaceAll("\\s+", " ").trim();
		String base = producto.getNombre() + " " + marca + " en Electrodental NEA. " + descripcion;
		return base.length() <= 155 ? base : base.substring(0, 152).trim() + "...";
	}

	private List<String> obtenerImagenesProducto(Producto producto) {
		List<String> imagenes = new ArrayList<>();
		if (producto.getImagenPrincipal() != null && !producto.getImagenPrincipal().isBlank()) {
			imagenes.add(producto.getImagenPrincipal().trim());
		}
		if (producto.getImagenesAdicionales() != null && !producto.getImagenesAdicionales().isBlank()) {
			producto.getImagenesAdicionales().lines()
					.map(String::trim)
					.filter(linea -> !linea.isBlank())
					.filter(linea -> !imagenes.contains(linea))
					.limit(Math.max(0, 10 - imagenes.size()))
					.forEach(imagenes::add);
		}
		if (imagenes.isEmpty()) {
			imagenes.add("/img/electrodental-logo-transparent.png");
		}
		return imagenes.stream().limit(10).toList();
	}

	private List<CaracteristicaProductoView> obtenerCaracteristicasProducto(Producto producto) {
		if (producto.getCaracteristicas() == null || producto.getCaracteristicas().isBlank()) {
			return List.of();
		}
		return producto.getCaracteristicas().lines()
				.map(String::trim)
				.filter(linea -> !linea.isBlank())
				.map(linea -> {
					String[] partes = linea.split("\\|", 2);
					if (partes.length == 2) {
						return new CaracteristicaProductoView(partes[0].trim(), partes[1].trim());
					}
					return new CaracteristicaProductoView(linea, "");
				})
				.toList();
	}

	public record CaracteristicaProductoView(String caracteristica, String detalle) {
	}
}
