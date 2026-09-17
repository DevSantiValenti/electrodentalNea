package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.util.StringUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
public class Curso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 180)
	private String titulo;

	@Column(nullable = false, unique = true, length = 220)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String descripcionBreve;

	@Column(length = 500)
	private String miniatura;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal duracionHoras = BigDecimal.ZERO;

	private Boolean activo = true;

	private Integer orden = 0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CertificadoModo certificadoModo = CertificadoModo.PUBLICO;

	@Column(length = 80)
	private String certificadoCodigo;

	@Column(length = 500)
	private String certificadoArchivo;

	@Column(length = 700)
	private String certificadoLinkExterno;

	@OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CursoClase> clases = new ArrayList<>();

	@PrePersist
	@PreUpdate
	public void completarDefaults() {
		if (duracionHoras == null || duracionHoras.signum() < 0) {
			duracionHoras = BigDecimal.ZERO;
		}
		if (activo == null) {
			activo = true;
		}
		if (orden == null) {
			orden = 0;
		}
		if (certificadoModo == null) {
			certificadoModo = CertificadoModo.PUBLICO;
		}
		if (StringUtils.hasText(slug)) {
			slug = normalizarSlug(slug);
		} else if (StringUtils.hasText(titulo)) {
			slug = normalizarSlug(titulo);
		}
		certificadoCodigo = normalizarCodigo(certificadoCodigo);
	}

	public boolean activoVisible() {
		return Boolean.TRUE.equals(activo);
	}

	public String getSlugUrl() {
		String slugNormalizado = normalizarSlug(slug);
		return StringUtils.hasText(slugNormalizado) ? slugNormalizado : slug;
	}

	public boolean requiereCodigoCertificado() {
		return certificadoModo == CertificadoModo.CODIGO;
	}

	public boolean tieneCertificadoDisponible() {
		return StringUtils.hasText(certificadoArchivo) || StringUtils.hasText(certificadoLinkExterno);
	}

	public boolean certificadoPorArchivo() {
		return StringUtils.hasText(certificadoArchivo);
	}

	public boolean certificadoPorLinkExterno() {
		return !certificadoPorArchivo() && StringUtils.hasText(certificadoLinkExterno);
	}

	public boolean codigoCertificadoValido(String codigo) {
		if (!requiereCodigoCertificado()) {
			return true;
		}
		return StringUtils.hasText(certificadoCodigo)
				&& certificadoCodigo.equals(normalizarCodigo(codigo));
	}

	public List<CursoClase> getClasesOrdenadasActivas() {
		if (clases == null) {
			return List.of();
		}
		return clases.stream()
				.filter(CursoClase::activoVisible)
				.sorted(Comparator.comparing(CursoClase::ordenSeguro).thenComparing(CursoClase::tituloSeguro))
				.toList();
	}

	public Optional<CursoClase> buscarClaseActiva(Long claseId) {
		if (claseId == null) {
			return Optional.empty();
		}
		return getClasesOrdenadasActivas().stream()
				.filter(clase -> claseId.equals(clase.getId()))
				.findFirst();
	}

	public void reemplazarClases(List<CursoClase> nuevasClases) {
		clases.clear();
		if (nuevasClases == null) {
			return;
		}
		nuevasClases.forEach(this::agregarClase);
	}

	public void agregarClase(CursoClase clase) {
		if (clase == null) {
			return;
		}
		clase.setCurso(this);
		clases.add(clase);
	}

	private static String normalizarCodigo(String codigo) {
		if (!StringUtils.hasText(codigo)) {
			return "";
		}
		return codigo.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
	}

	public static String normalizarSlug(String valor) {
		if (!StringUtils.hasText(valor)) {
			return "";
		}
		String sinAcentos = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return sinAcentos.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
	}
}
