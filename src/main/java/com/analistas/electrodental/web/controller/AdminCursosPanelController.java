package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.CertificadoModo;
import com.analistas.electrodental.model.domain.Curso;
import com.analistas.electrodental.model.domain.CursoClase;
import com.analistas.electrodental.model.service.ICursoService;
import com.analistas.electrodental.model.service.ProductoImagenStorageService;

@Controller
public class AdminCursosPanelController {

	private final ICursoService cursoService;
	private final ProductoImagenStorageService productoImagenStorageService;

	public AdminCursosPanelController(
			ICursoService cursoService,
			ProductoImagenStorageService productoImagenStorageService) {
		this.cursoService = cursoService;
		this.productoImagenStorageService = productoImagenStorageService;
	}

	@GetMapping("/admin/cursos-panel/login")
	public String loginCursos(Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			return "redirect:/admin/cursos-panel";
		}
		return "admin/cursos-panel/login";
	}

	@GetMapping("/admin/cursos-panel")
	public String dashboard(Model model) {
		model.addAttribute("totalCursosActivos", cursoService.contarActivos());
		model.addAttribute("totalClasesActivas", cursoService.contarClasesActivas());
		model.addAttribute("cursos", cursoService.listarTodos().stream().limit(6).toList());
		return "admin/cursos-panel/dashboard";
	}

	@GetMapping("/admin/cursos-panel/cursos")
	public String cursos(Model model) {
		model.addAttribute("cursos", cursoService.listarTodos());
		return "admin/cursos-panel/cursos";
	}

	@GetMapping("/admin/cursos-panel/cursos/nuevo")
	public String nuevoCurso(Model model) {
		Curso curso = new Curso();
		curso.setCertificadoModo(CertificadoModo.PUBLICO);
		curso.agregarClase(claseVacia());
		cargarFormularioCurso(model, curso);
		return "admin/cursos-panel/curso-form";
	}

	@PostMapping("/admin/cursos-panel/cursos")
	public String guardarCurso(
			Curso curso,
			@RequestParam(name = "miniaturaArchivo", required = false) MultipartFile miniaturaArchivo,
			@RequestParam(name = "certificadoArchivoUpload", required = false) MultipartFile certificadoArchivoUpload,
			@RequestParam(required = false) List<String> claseTitulos,
			@RequestParam(required = false) List<String> claseDescripciones,
			@RequestParam(required = false) List<String> claseYoutubeUrls,
			@RequestParam(required = false) List<Integer> claseOrdenes,
			@RequestParam(required = false) List<Integer> claseActivas,
			Model model,
			RedirectAttributes redirectAttributes) {
		return guardarCursoDesdeFormulario(
				curso,
				miniaturaArchivo,
				certificadoArchivoUpload,
				claseTitulos,
				claseDescripciones,
				claseYoutubeUrls,
				claseOrdenes,
				claseActivas,
				model,
				redirectAttributes,
				"Curso guardado correctamente",
				"No se pudo guardar el curso.");
	}

	@GetMapping("/admin/cursos-panel/cursos/{id}/editar")
	public String editarCurso(@PathVariable Long id, Model model) {
		Curso curso = cursoService.buscarPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + id));
		cargarFormularioCurso(model, curso);
		return "admin/cursos-panel/curso-form";
	}

	@PostMapping("/admin/cursos-panel/cursos/{id}")
	public String actualizarCurso(
			@PathVariable Long id,
			Curso curso,
			@RequestParam(name = "miniaturaArchivo", required = false) MultipartFile miniaturaArchivo,
			@RequestParam(name = "certificadoArchivoUpload", required = false) MultipartFile certificadoArchivoUpload,
			@RequestParam(required = false) List<String> claseTitulos,
			@RequestParam(required = false) List<String> claseDescripciones,
			@RequestParam(required = false) List<String> claseYoutubeUrls,
			@RequestParam(required = false) List<Integer> claseOrdenes,
			@RequestParam(required = false) List<Integer> claseActivas,
			Model model,
			RedirectAttributes redirectAttributes) {
		curso.setId(id);
		return guardarCursoDesdeFormulario(
				curso,
				miniaturaArchivo,
				certificadoArchivoUpload,
				claseTitulos,
				claseDescripciones,
				claseYoutubeUrls,
				claseOrdenes,
				claseActivas,
				model,
				redirectAttributes,
				"Curso actualizado correctamente",
				"No se pudo actualizar el curso.");
	}

	@PostMapping("/admin/cursos-panel/cursos/{id}/eliminar")
	public String eliminarCurso(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		cursoService.desactivar(id);
		redirectAttributes.addFlashAttribute("mensaje", "Curso desactivado correctamente.");
		return "redirect:/admin/cursos-panel/cursos";
	}

	private String guardarCursoDesdeFormulario(
			Curso curso,
			MultipartFile miniaturaArchivo,
			MultipartFile certificadoArchivoUpload,
			List<String> claseTitulos,
			List<String> claseDescripciones,
			List<String> claseYoutubeUrls,
			List<Integer> claseOrdenes,
			List<Integer> claseActivas,
			Model model,
			RedirectAttributes redirectAttributes,
			String mensajeOk,
			String mensajeErrorBase) {
		try {
			prepararCurso(curso, miniaturaArchivo, certificadoArchivoUpload, claseTitulos, claseDescripciones, claseYoutubeUrls, claseOrdenes, claseActivas);
		} catch (CursoArchivoException ex) {
			cargarFormularioCursoConError(model, curso, Map.of(ex.campo, ex.getMessage()), mensajeErrorBase);
			return "admin/cursos-panel/curso-form";
		} catch (RuntimeException ex) {
			cargarFormularioCursoConError(model, curso, Map.of("general", mensajeCadena(ex)), mensajeErrorBase);
			return "admin/cursos-panel/curso-form";
		}
		Map<String, String> errores = validarCurso(curso);
		if (!errores.isEmpty()) {
			cargarFormularioCursoConError(model, curso, errores, "Revisá los campos marcados.");
			return "admin/cursos-panel/curso-form";
		}
		try {
			cursoService.guardar(curso);
			redirectAttributes.addFlashAttribute("mensaje", mensajeOk);
			return "redirect:/admin/cursos-panel/cursos";
		} catch (DataIntegrityViolationException ex) {
			cargarFormularioCursoConError(model, curso, Map.of("slug", "El slug ya existe. Usá uno distinto."), mensajeErrorBase);
			return "admin/cursos-panel/curso-form";
		} catch (RuntimeException ex) {
			cargarFormularioCursoConError(model, curso, Map.of("general", mensajeCadena(ex)), mensajeErrorBase);
			return "admin/cursos-panel/curso-form";
		}
	}

	private void prepararCurso(
			Curso curso,
			MultipartFile miniaturaArchivo,
			MultipartFile certificadoArchivoUpload,
			List<String> claseTitulos,
			List<String> claseDescripciones,
			List<String> claseYoutubeUrls,
			List<Integer> claseOrdenes,
			List<Integer> claseActivas) {
		curso.setTitulo(normalizarValorSimple(curso.getTitulo()));
		curso.setSlug(generarSlug(StringUtils.hasText(curso.getSlug()) ? curso.getSlug() : curso.getTitulo()));
		curso.setDescripcionBreve(normalizarValorSimple(curso.getDescripcionBreve()));
		curso.setMiniatura(normalizarValorSimple(curso.getMiniatura()));
		curso.setCertificadoArchivo(normalizarValorSimple(curso.getCertificadoArchivo()));
		curso.setCertificadoLinkExterno(normalizarValorSimple(curso.getCertificadoLinkExterno()));
		curso.setCertificadoCodigo(normalizarValorSimple(curso.getCertificadoCodigo()));
		curso.setActivo(curso.getActivo() != null && curso.getActivo());
		curso.setOrden(curso.getOrden() == null ? 0 : curso.getOrden());
		curso.setDuracionHoras(curso.getDuracionHoras() == null ? BigDecimal.ZERO : curso.getDuracionHoras());
		curso.setCertificadoModo(curso.getCertificadoModo() == null ? CertificadoModo.PUBLICO : curso.getCertificadoModo());
		if (miniaturaArchivo != null && !miniaturaArchivo.isEmpty()) {
			try {
				curso.setMiniatura(productoImagenStorageService.guardarCursoMiniatura(miniaturaArchivo));
			} catch (RuntimeException ex) {
				throw new CursoArchivoException("miniaturaArchivo", ex.getMessage(), ex);
			}
		}
		if (certificadoArchivoUpload != null && !certificadoArchivoUpload.isEmpty()) {
			try {
				curso.setCertificadoArchivo(productoImagenStorageService.guardarCursoCertificado(certificadoArchivoUpload));
			} catch (RuntimeException ex) {
				throw new CursoArchivoException("certificadoArchivoUpload", ex.getMessage(), ex);
			}
		}
		curso.reemplazarClases(clasesDesdeFormulario(claseTitulos, claseDescripciones, claseYoutubeUrls, claseOrdenes, claseActivas));
	}

	private static class CursoArchivoException extends RuntimeException {
		private final String campo;

		CursoArchivoException(String campo, String mensaje, Throwable cause) {
			super(mensaje, cause);
			this.campo = campo;
		}
	}

	private Map<String, String> validarCurso(Curso curso) {
		Map<String, String> errores = new LinkedHashMap<>();
		if (!StringUtils.hasText(curso.getTitulo())) {
			errores.put("titulo", "El título es obligatorio.");
		}
		if (!StringUtils.hasText(curso.getSlug())) {
			errores.put("slug", "El slug es obligatorio.");
		}
		if (curso.getDuracionHoras() != null && curso.getDuracionHoras().signum() < 0) {
			errores.put("duracionHoras", "La duración no puede ser negativa.");
		}
		if (curso.requiereCodigoCertificado() && !StringUtils.hasText(curso.getCertificadoCodigo())) {
			errores.put("certificadoCodigo", "El código es obligatorio cuando el certificado requiere código.");
		}
		if (StringUtils.hasText(curso.getCertificadoLinkExterno()) && !esUrlHttp(curso.getCertificadoLinkExterno())) {
			errores.put("certificadoLinkExterno", "Ingresá un link válido que empiece con http:// o https://.");
		}
		IntStream.range(0, curso.getClases().size()).forEach(indice -> {
			CursoClase clase = curso.getClases().get(indice);
			if (!StringUtils.hasText(clase.getTitulo())) {
				errores.put("claseTitulo" + indice, "Cada clase cargada necesita título.");
			}
			if (!CursoClase.esYoutubeValido(clase.getYoutubeUrl())) {
				errores.put("claseYoutube" + indice, "Cada clase cargada necesita un link válido de YouTube.");
			}
		});
		return errores;
	}

	private List<CursoClase> clasesDesdeFormulario(
			List<String> titulos,
			List<String> descripciones,
			List<String> youtubeUrls,
			List<Integer> ordenes,
			List<Integer> activas) {
		if (titulos == null || titulos.isEmpty()) {
			return List.of();
		}
		List<CursoClase> clases = new ArrayList<>();
		for (int indice = 0; indice < titulos.size(); indice++) {
			String titulo = valorEn(titulos, indice);
			String descripcion = valorEn(descripciones, indice);
			String youtubeUrl = valorEn(youtubeUrls, indice);
			if (!StringUtils.hasText(titulo) && !StringUtils.hasText(descripcion) && !StringUtils.hasText(youtubeUrl)) {
				continue;
			}
			CursoClase clase = new CursoClase();
			clase.setTitulo(titulo);
			clase.setDescripcion(descripcion);
			clase.setYoutubeUrl(youtubeUrl);
			clase.setOrden(numeroEn(ordenes, indice, indice + 1));
			clase.setActivo(activas != null && activas.contains(indice));
			clases.add(clase);
		}
		return clases;
	}

	private void cargarFormularioCurso(Model model, Curso curso) {
		model.addAttribute("curso", curso);
		model.addAttribute("modosCertificado", CertificadoModo.values());
		model.addAttribute("clasesCurso", clasesFormulario(curso));
	}

	private void cargarFormularioCursoConError(Model model, Curso curso, Map<String, String> errores, String mensajeError) {
		cargarFormularioCurso(model, curso);
		model.addAttribute("erroresCurso", errores);
		model.addAttribute("mensajeError", mensajeError);
	}

	private List<CursoClase> clasesFormulario(Curso curso) {
		if (curso.getClases() == null || curso.getClases().isEmpty()) {
			return List.of(claseVacia());
		}
		return curso.getClases().stream()
				.sorted((a, b) -> a.ordenSeguro().compareTo(b.ordenSeguro()))
				.toList();
	}

	private CursoClase claseVacia() {
		CursoClase clase = new CursoClase();
		clase.setActivo(true);
		clase.setOrden(1);
		return clase;
	}

	private boolean esUrlHttp(String url) {
		try {
			URI uri = URI.create(url);
			return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private String generarSlug(String nombre) {
		String slug = Curso.normalizarSlug(nombre);
		if (!StringUtils.hasText(slug)) {
			return "curso-" + System.currentTimeMillis();
		}
		return slug;
	}

	private String normalizarValorSimple(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private String valorEn(List<String> valores, int indice) {
		return valores != null && valores.size() > indice ? normalizarValorSimple(valores.get(indice)) : "";
	}

	private Integer numeroEn(List<Integer> valores, int indice, int defaultValue) {
		return valores != null && valores.size() > indice && valores.get(indice) != null ? valores.get(indice) : defaultValue;
	}

	private String mensajeCadena(Throwable throwable) {
		List<String> mensajes = new ArrayList<>();
		Throwable actual = throwable;
		while (actual != null) {
			if (StringUtils.hasText(actual.getMessage())) {
				mensajes.add(actual.getMessage());
			}
			actual = actual.getCause();
		}
		return String.join(" | ", mensajes);
	}
}
