package com.analistas.electrodental.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.service.ICategoriaService;

@Controller
public class AdminCategoriaController {

	private final ICategoriaService categoriaService;

	public AdminCategoriaController(ICategoriaService categoriaService) {
		this.categoriaService = categoriaService;
	}

	@GetMapping("/admin/categorias")
	public String categorias(Model model) {
		model.addAttribute("categorias", categoriaService.listarTodas());
		return "admin/categorias";
	}

	@GetMapping("/admin/categorias/nueva")
	public String nuevaCategoria(Model model) {
		model.addAttribute("categoria", new Categoria());
		return "admin/categoria-form";
	}

	@PostMapping("/admin/categorias")
	public String guardarCategoria(Categoria categoria, RedirectAttributes redirectAttributes) {
		prepararCategoria(categoria);
		categoriaService.guardarCategoria(categoria);
		redirectAttributes.addFlashAttribute("mensaje", "Categoría guardada correctamente");
		return "redirect:/admin/categorias";
	}

	@GetMapping("/admin/categorias/{id}/editar")
	public String editarCategoria(@PathVariable Long id, Model model) {
		Categoria categoria = categoriaService.buscarCategoriaPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + id));
		model.addAttribute("categoria", categoria);
		return "admin/categoria-form";
	}

	@PostMapping("/admin/categorias/{id}")
	public String actualizarCategoria(@PathVariable Long id, Categoria categoria, RedirectAttributes redirectAttributes) {
		categoria.setId(id);
		prepararCategoria(categoria);
		categoriaService.guardarCategoria(categoria);
		redirectAttributes.addFlashAttribute("mensaje", "Categoría actualizada correctamente");
		return "redirect:/admin/categorias";
	}

	@GetMapping("/admin/categorias/{categoriaId}/subcategorias/nueva")
	public String nuevaSubcategoria(@PathVariable Long categoriaId, Model model) {
		Categoria categoria = categoriaService.buscarCategoriaPorId(categoriaId)
				.orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + categoriaId));
		Subcategoria subcategoria = new Subcategoria();
		subcategoria.setCategoria(categoria);
		model.addAttribute("categoria", categoria);
		model.addAttribute("subcategoria", subcategoria);
		return "admin/subcategoria-form";
	}

	@PostMapping("/admin/categorias/{categoriaId}/subcategorias")
	public String guardarSubcategoria(@PathVariable Long categoriaId, Subcategoria subcategoria, RedirectAttributes redirectAttributes) {
		prepararSubcategoria(subcategoria);
		categoriaService.guardarSubcategoria(categoriaId, subcategoria);
		redirectAttributes.addFlashAttribute("mensaje", "Subcategoría guardada correctamente");
		return "redirect:/admin/categorias";
	}

	@GetMapping("/admin/subcategorias/{id}/editar")
	public String editarSubcategoria(@PathVariable Long id, Model model) {
		Subcategoria subcategoria = categoriaService.buscarSubcategoriaPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Subcategoria no encontrada: " + id));
		model.addAttribute("categoria", subcategoria.getCategoria());
		model.addAttribute("subcategoria", subcategoria);
		return "admin/subcategoria-form";
	}

	@PostMapping("/admin/subcategorias/{id}")
	public String actualizarSubcategoria(@PathVariable Long id, Subcategoria subcategoria, RedirectAttributes redirectAttributes) {
		Subcategoria actual = categoriaService.buscarSubcategoriaPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Subcategoria no encontrada: " + id));
		subcategoria.setId(id);
		subcategoria.setCategoria(actual.getCategoria());
		prepararSubcategoria(subcategoria);
		categoriaService.guardarSubcategoria(actual.getCategoria().getId(), subcategoria);
		redirectAttributes.addFlashAttribute("mensaje", "Subcategoría actualizada correctamente");
		return "redirect:/admin/categorias";
	}

	private void prepararCategoria(Categoria categoria) {
		if (categoria.getSlug() == null || categoria.getSlug().isBlank()) {
			categoria.setSlug(generarSlug(categoria.getNombre()));
		}
		categoria.setActivo(categoria.getActivo() != null && categoria.getActivo());
	}

	private void prepararSubcategoria(Subcategoria subcategoria) {
		if (subcategoria.getSlug() == null || subcategoria.getSlug().isBlank()) {
			subcategoria.setSlug(generarSlug(subcategoria.getNombre()));
		}
		subcategoria.setActivo(subcategoria.getActivo() != null && subcategoria.getActivo());
	}

	private String generarSlug(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			return "item-" + System.currentTimeMillis();
		}
		return nombre.toLowerCase()
				.replaceAll("[^a-z0-9áéíóúñ]+", "-")
				.replaceAll("^-|-$", "");
	}
}
