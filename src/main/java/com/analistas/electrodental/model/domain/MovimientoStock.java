package com.analistas.electrodental.model.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id", nullable = false)
	private Producto producto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoMovimientoStock tipo;

	@Column(nullable = false)
	private Integer cantidad;

	private Integer stockAnteriorWeb;

	private Integer stockNuevoWeb;

	private Integer stockAnteriorFisico;

	private Integer stockNuevoFisico;

	@Column(length = 160)
	private String referencia;

	private LocalDateTime fecha;

	@PrePersist
	public void prePersist() {
		fecha = fecha == null ? LocalDateTime.now() : fecha;
	}
}
