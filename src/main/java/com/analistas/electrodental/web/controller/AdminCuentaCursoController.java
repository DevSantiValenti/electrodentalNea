package com.analistas.electrodental.web.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.CursoPanelUsuario;
import com.analistas.electrodental.model.service.ICursoPanelUsuarioService;

@Controller
public class AdminCuentaCursoController {

	private final ICursoPanelUsuarioService usuarioService;

	public AdminCuentaCursoController(ICursoPanelUsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/admin/cuentas-cursos")
	public String cuentas(Model model) {
		model.addAttribute("usuariosCursos", usuarioService.listarTodos());
		return "admin/cuentas-cursos";
	}

	@GetMapping("/admin/cuentas-cursos/nueva")
	public String nuevaCuenta(Model model) {
		CursoPanelUsuario usuario = new CursoPanelUsuario();
		usuario.setActivo(true);
		cargarFormulario(model, usuario);
		return "admin/cuenta-curso-form";
	}

	@PostMapping("/admin/cuentas-cursos")
	public String guardarCuenta(
			CursoPanelUsuario usuario,
			@RequestParam(required = false) String password,
			Model model,
			RedirectAttributes redirectAttributes) {
		Map<String, String> errores = validar(usuario, password, true);
		if (!errores.isEmpty()) {
			cargarFormularioConError(model, usuario, errores);
			return "admin/cuenta-curso-form";
		}
		try {
			usuarioService.guardar(usuario, password);
			redirectAttributes.addFlashAttribute("mensaje", "Cuenta de cursos creada correctamente.");
			return "redirect:/admin/cuentas-cursos";
		} catch (DataIntegrityViolationException ex) {
			cargarFormularioConError(model, usuario, Map.of("usuario", "Ya existe una cuenta con ese usuario."));
			return "admin/cuenta-curso-form";
		}
	}

	@GetMapping("/admin/cuentas-cursos/{id}/editar")
	public String editarCuenta(@PathVariable Long id, Model model) {
		CursoPanelUsuario usuario = usuarioService.buscarPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + id));
		cargarFormulario(model, usuario);
		return "admin/cuenta-curso-form";
	}

	@PostMapping("/admin/cuentas-cursos/{id}")
	public String actualizarCuenta(
			@PathVariable Long id,
			CursoPanelUsuario usuario,
			@RequestParam(required = false) String password,
			Model model,
			RedirectAttributes redirectAttributes) {
		usuario.setId(id);
		Map<String, String> errores = validar(usuario, password, false);
		if (!errores.isEmpty()) {
			cargarFormularioConError(model, usuario, errores);
			return "admin/cuenta-curso-form";
		}
		try {
			usuarioService.guardar(usuario, password);
			redirectAttributes.addFlashAttribute("mensaje", "Cuenta de cursos actualizada correctamente.");
			return "redirect:/admin/cuentas-cursos";
		} catch (DataIntegrityViolationException ex) {
			cargarFormularioConError(model, usuario, Map.of("usuario", "Ya existe una cuenta con ese usuario."));
			return "admin/cuenta-curso-form";
		}
	}

	@PostMapping("/admin/cuentas-cursos/{id}/eliminar")
	public String eliminarCuenta(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		usuarioService.desactivar(id);
		redirectAttributes.addFlashAttribute("mensaje", "Cuenta de cursos desactivada.");
		return "redirect:/admin/cuentas-cursos";
	}

	private Map<String, String> validar(CursoPanelUsuario usuario, String password, boolean nueva) {
		Map<String, String> errores = new LinkedHashMap<>();
		if (!StringUtils.hasText(usuario.getNombre())) {
			errores.put("nombre", "El nombre es obligatorio.");
		}
		if (!StringUtils.hasText(usuario.getUsuario())) {
			errores.put("usuario", "El usuario es obligatorio.");
		}
		if (nueva && !StringUtils.hasText(password)) {
			errores.put("password", "La contraseña inicial es obligatoria.");
		}
		return errores;
	}

	private void cargarFormulario(Model model, CursoPanelUsuario usuario) {
		model.addAttribute("usuarioCurso", usuario);
	}

	private void cargarFormularioConError(Model model, CursoPanelUsuario usuario, Map<String, String> errores) {
		cargarFormulario(model, usuario);
		model.addAttribute("erroresCuentaCurso", errores);
		model.addAttribute("mensajeError", "Revisá los campos marcados.");
	}
}
