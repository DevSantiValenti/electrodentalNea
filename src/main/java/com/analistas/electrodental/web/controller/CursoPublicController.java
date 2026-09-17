package com.analistas.electrodental.web.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.Curso;
import com.analistas.electrodental.model.domain.CursoClase;
import com.analistas.electrodental.model.service.ICursoService;
import com.analistas.electrodental.model.service.IProductoService;
import com.analistas.electrodental.model.service.ProductoImagenStorageService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CursoPublicController {

	private static final String CERTIFICADO_SESSION_PREFIX = "cursoCertificadoAutorizado:";

	private final ICursoService cursoService;
	private final IProductoService productoService;
	private final ProductoImagenStorageService productoImagenStorageService;

	public CursoPublicController(
			ICursoService cursoService,
			IProductoService productoService,
			ProductoImagenStorageService productoImagenStorageService) {
		this.cursoService = cursoService;
		this.productoService = productoService;
		this.productoImagenStorageService = productoImagenStorageService;
	}

	@GetMapping({ "/cursos", "/capacitaciones" })
	public String cursos(Model model) {
		model.addAttribute("cursos", cursoService.listarActivos());
		model.addAttribute("seoDescription", "Cursos y capacitaciones de Electrodental NEA con clases grabadas, certificados y productos odontologicos recomendados.");
		return "cursos/listado";
	}

	@GetMapping({ "/cursos/{slug}", "/capacitaciones/{slug}" })
	public String detalle(@PathVariable String slug, Model model, HttpSession session) {
		Curso curso = cursoService.buscarActivoPorSlug(slug).orElse(null);
		model.addAttribute("curso", curso);
		model.addAttribute("certificadoAutorizado", curso != null && certificadoAutorizado(curso, session));
		model.addAttribute("seoDescription", curso == null
				? "Curso no encontrado en Electrodental NEA."
				: curso.getTitulo() + " en Electrodental NEA: clases grabadas, capacitacion y certificado.");
		return "cursos/detalle";
	}

	@GetMapping({ "/cursos/{slug}/clases/{claseId}", "/capacitaciones/{slug}/clases/{claseId}" })
	public String clase(
			@PathVariable String slug,
			@PathVariable Long claseId,
			Model model,
			HttpSession session) {
		Curso curso = cursoService.buscarActivoPorSlug(slug).orElse(null);
		List<CursoClase> clases = curso == null ? List.of() : curso.getClasesOrdenadasActivas();
		CursoClase clase = curso == null ? null : curso.buscarClaseActiva(claseId).orElse(null);
		int indice = clase == null ? -1 : clases.indexOf(clase);
		model.addAttribute("curso", curso);
		model.addAttribute("clase", clase);
		model.addAttribute("clasesCurso", clases);
		model.addAttribute("claseNumero", indice >= 0 ? indice + 1 : 0);
		model.addAttribute("claseAnterior", indice > 0 ? clases.get(indice - 1) : null);
		model.addAttribute("claseSiguiente", indice >= 0 && indice < clases.size() - 1 ? clases.get(indice + 1) : null);
		model.addAttribute("productosCursos", productoService.listarParaCursos());
		model.addAttribute("certificadoAutorizado", curso != null && certificadoAutorizado(curso, session));
		model.addAttribute("seoDescription", curso == null || clase == null
				? "Clase no encontrada en Electrodental NEA."
				: clase.getTitulo() + " de " + curso.getTitulo() + " en Electrodental NEA.");
		return "cursos/clase";
	}

	@PostMapping({ "/cursos/{slug}/certificado", "/capacitaciones/{slug}/certificado" })
	public String validarCertificado(
			@PathVariable String slug,
			@RequestParam(required = false) String codigo,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Curso curso = cursoService.buscarActivoPorSlug(slug).orElse(null);
		if (curso == null || !curso.tieneCertificadoDisponible()) {
			redirectAttributes.addFlashAttribute("mensajeError", "El certificado no está disponible.");
			return redirectCurso(redirectAttributes, slug);
		}
		if (curso.codigoCertificadoValido(codigo)) {
			session.setAttribute(sessionKey(curso), true);
			return redirectCertificado(redirectAttributes, curso);
		}
		redirectAttributes.addFlashAttribute("mensajeError", "El código ingresado no es válido.");
		return redirectUltimaClaseOCurso(redirectAttributes, curso);
	}

	@GetMapping({ "/cursos/{slug}/certificado/descargar", "/capacitaciones/{slug}/certificado/descargar" })
	public Object descargarCertificado(
			@PathVariable String slug,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Curso curso = cursoService.buscarActivoPorSlug(slug).orElse(null);
		if (curso == null || !curso.tieneCertificadoDisponible()) {
			redirectAttributes.addFlashAttribute("mensajeError", "El certificado no está disponible.");
			return redirectCurso(redirectAttributes, slug);
		}
		if (!certificadoAutorizado(curso, session)) {
			redirectAttributes.addFlashAttribute("mensajeError", "Ingresá el código para descargar el certificado.");
			return redirectUltimaClaseOCurso(redirectAttributes, curso);
		}
		if (curso.certificadoPorLinkExterno()) {
			return "redirect:" + curso.getCertificadoLinkExterno();
		}
		try {
			Path archivo = productoImagenStorageService.resolverCursoCertificado(curso.getCertificadoArchivo());
			if (!Files.isRegularFile(archivo)) {
				redirectAttributes.addFlashAttribute("mensajeError", "El archivo del certificado no se encontró.");
				return redirectUltimaClaseOCurso(redirectAttributes, curso);
			}
			Resource resource = new UrlResource(archivo.toUri());
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
							.filename("certificado-" + curso.getSlugUrl() + ".pdf")
							.build()
							.toString())
					.body(resource);
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("mensajeError", "No se pudo descargar el certificado.");
			return redirectUltimaClaseOCurso(redirectAttributes, curso);
		}
	}

	private String redirectCurso(RedirectAttributes redirectAttributes, Curso curso) {
		redirectAttributes.addAttribute("slug", curso.getSlugUrl());
		return "redirect:/cursos/{slug}";
	}

	private String redirectCurso(RedirectAttributes redirectAttributes, String slug) {
		redirectAttributes.addAttribute("slug", Curso.normalizarSlug(slug));
		return "redirect:/cursos/{slug}";
	}

	private String redirectCertificado(RedirectAttributes redirectAttributes, Curso curso) {
		redirectAttributes.addAttribute("slug", curso.getSlugUrl());
		return "redirect:/cursos/{slug}/certificado/descargar";
	}

	private String redirectUltimaClaseOCurso(RedirectAttributes redirectAttributes, Curso curso) {
		List<CursoClase> clases = curso.getClasesOrdenadasActivas();
		if (clases.isEmpty()) {
			return redirectCurso(redirectAttributes, curso);
		}
		redirectAttributes.addAttribute("slug", curso.getSlugUrl());
		redirectAttributes.addAttribute("claseId", clases.get(clases.size() - 1).getId());
		return "redirect:/cursos/{slug}/clases/{claseId}";
	}

	private boolean certificadoAutorizado(Curso curso, HttpSession session) {
		return curso != null && (!curso.requiereCodigoCertificado()
				|| Boolean.TRUE.equals(session.getAttribute(sessionKey(curso))));
	}

	private String sessionKey(Curso curso) {
		return CERTIFICADO_SESSION_PREFIX + (curso.getId() == null ? curso.getSlugUrl() : curso.getId());
	}
}
