package com.analistas.electrodental.web.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.DescuentoCodigo;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.domain.TipoAplicacionDescuento;
import com.analistas.electrodental.model.repository.ISubcategoriaRepository;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.IDescuentoService;
import com.analistas.electrodental.model.service.IProductoService;

@Controller
public class AdminDescuentoController {

	private final IDescuentoService descuentoService;
	private final IProductoService productoService;
	private final ICategoriaService categoriaService;
	private final ISubcategoriaRepository subcategoriaRepository;

	public AdminDescuentoController(
			IDescuentoService descuentoService,
			IProductoService productoService,
			ICategoriaService categoriaService,
			ISubcategoriaRepository subcategoriaRepository) {
		this.descuentoService = descuentoService;
		this.productoService = productoService;
		this.categoriaService = categoriaService;
		this.subcategoriaRepository = subcategoriaRepository;
	}

	@GetMapping("/admin/descuentos")
	public String listar(Model model) {
		model.addAttribute("descuentos", descuentoService.listarTodos());
		return "admin/descuentos";
	}

	@GetMapping("/admin/descuentos/nuevo")
	public String nuevo(Model model) {
		cargarFormulario(model, descuentoService.nuevoDescuento());
		return "admin/descuento-form";
	}

	@PostMapping("/admin/descuentos")
	public String guardar(
			DescuentoCodigo descuento,
			@RequestParam(required = false) List<Long> productoIds,
			@RequestParam(required = false) List<Long> categoriaIds,
			@RequestParam(required = false) List<Long> subcategoriaIds,
			RedirectAttributes redirectAttributes) {
		try {
			descuentoService.guardar(null, descuento, productoIds, categoriaIds, subcategoriaIds);
			redirectAttributes.addFlashAttribute("mensaje", "Descuento guardado correctamente.");
			return "redirect:/admin/descuentos";
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo guardar el descuento: " + ex.getMessage());
			return "redirect:/admin/descuentos/nuevo";
		}
	}

	@GetMapping("/admin/descuentos/{id}/editar")
	public String editar(@PathVariable Long id, Model model) {
		DescuentoCodigo descuento = descuentoService.buscarPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Descuento no encontrado: " + id));
		cargarFormulario(model, descuento);
		return "admin/descuento-form";
	}

	@PostMapping("/admin/descuentos/{id}")
	public String actualizar(
			@PathVariable Long id,
			DescuentoCodigo descuento,
			@RequestParam(required = false) List<Long> productoIds,
			@RequestParam(required = false) List<Long> categoriaIds,
			@RequestParam(required = false) List<Long> subcategoriaIds,
			RedirectAttributes redirectAttributes) {
		try {
			descuentoService.guardar(id, descuento, productoIds, categoriaIds, subcategoriaIds);
			redirectAttributes.addFlashAttribute("mensaje", "Descuento actualizado correctamente.");
			return "redirect:/admin/descuentos";
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo actualizar el descuento: " + ex.getMessage());
			return "redirect:/admin/descuentos/" + id + "/editar";
		}
	}

	@PostMapping("/admin/descuentos/{id}/eliminar")
	public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			descuentoService.eliminar(id);
			redirectAttributes.addFlashAttribute("mensaje", "Descuento eliminado correctamente.");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo eliminar el descuento: " + ex.getMessage());
		}
		return "redirect:/admin/descuentos";
	}

	private void cargarFormulario(Model model, DescuentoCodigo descuento) {
		List<Categoria> categorias = categoriaService.listarTodas();
		model.addAttribute("descuento", descuento);
		model.addAttribute("tiposAplicacion", TipoAplicacionDescuento.values());
		model.addAttribute("productos", productoService.listarTodos().stream()
				.filter(producto -> !producto.tieneOferta())
				.toList());
		model.addAttribute("categorias", categorias);
		model.addAttribute("subcategorias", subcategoriaRepository.findByActivoTrueOrderByNombreAsc());
		model.addAttribute("productoIdsSeleccionados", idsProductos(descuento));
		model.addAttribute("categoriaIdsSeleccionadas", idsCategorias(descuento));
		model.addAttribute("subcategoriaIdsSeleccionadas", idsSubcategorias(descuento));
	}

	private Set<Long> idsProductos(DescuentoCodigo descuento) {
		return descuento.getProductos().stream()
				.map(Producto::getId)
				.collect(Collectors.toSet());
	}

	private Set<Long> idsCategorias(DescuentoCodigo descuento) {
		return descuento.getCategorias().stream()
				.map(Categoria::getId)
				.collect(Collectors.toSet());
	}

	private Set<Long> idsSubcategorias(DescuentoCodigo descuento) {
		return descuento.getSubcategorias().stream()
				.map(Subcategoria::getId)
				.collect(Collectors.toSet());
	}
}
