package com.analistas.electrodental.model.domain;

import java.time.LocalDateTime;

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
@Table(name = "curso_panel_usuarios")
@Getter
@Setter
@NoArgsConstructor
public class CursoPanelUsuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, unique = true, length = 80)
	private String usuario;

	@Column(nullable = false, length = 120)
	private String passwordHash;

	private Boolean activo = true;

	@Column(nullable = false)
	private LocalDateTime creadoEn;

	private LocalDateTime actualizadoEn;

	@PrePersist
	public void antesDeCrear() {
		if (creadoEn == null) {
			creadoEn = LocalDateTime.now();
		}
		completarDefaults();
	}

	@PreUpdate
	public void antesDeActualizar() {
		actualizadoEn = LocalDateTime.now();
		completarDefaults();
	}

	public void completarDefaults() {
		if (!StringUtils.hasText(nombre)) {
			nombre = usuario;
		}
		if (usuario != null) {
			usuario = usuario.trim().toLowerCase();
		}
		if (activo == null) {
			activo = true;
		}
	}
}
