package com.analistas.electrodental.model.domain;

import org.springframework.util.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "curso_areas")
@Getter
@Setter
@NoArgsConstructor
public class CursoArea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 140)
	private String nombre;

	@Column(nullable = false, unique = true, length = 160)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	private Boolean activo = true;

	private Integer orden = 0;

	@PrePersist
	@PreUpdate
	public void completarDefaults() {
		nombre = normalizarTexto(nombre);
		descripcion = normalizarTexto(descripcion);
		if (StringUtils.hasText(slug)) {
			slug = Curso.normalizarSlug(slug);
		} else if (StringUtils.hasText(nombre)) {
			slug = Curso.normalizarSlug(nombre);
		}
		if (activo == null) {
			activo = true;
		}
		if (orden == null) {
			orden = 0;
		}
	}

	public boolean activoVisible() {
		return Boolean.TRUE.equals(activo);
	}

	public Integer ordenSeguro() {
		return orden == null ? 0 : orden;
	}

	public String nombreSeguro() {
		return nombre == null ? "" : nombre;
	}

	private String normalizarTexto(String valor) {
		return valor == null ? "" : valor.trim();
	}
}
