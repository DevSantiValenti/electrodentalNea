package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 180)
	private String nombre;

	@Column(nullable = false, unique = true, length = 220)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(length = 120)
	private String modelo;

	@Column(columnDefinition = "TEXT")
	private String caracteristicas;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal precio = BigDecimal.ZERO;

	@Column(length = 120)
	private String marca;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "categoria_id")
	private Categoria categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subcategoria_id")
	private Subcategoria subcategoria;

	@Column(length = 500)
	private String imagenPrincipal;

	@Column(columnDefinition = "TEXT")
	private String imagenesAdicionales;

	private Boolean activo = true;

	private Boolean destacado = false;

	private Boolean oferta = false;

	@Column(precision = 5, scale = 2)
	private BigDecimal porcentajeOferta = BigDecimal.ZERO;

	private Integer stockWeb = 0;

	private Integer stockFisico = 0;

	private Integer stockMinimo = 3;

	@Column(precision = 10, scale = 3)
	private BigDecimal pesoKg = BigDecimal.ZERO;

	@Column(precision = 10, scale = 2)
	private BigDecimal altoCm = BigDecimal.ZERO;

	@Column(precision = 10, scale = 2)
	private BigDecimal anchoCm = BigDecimal.ZERO;

	@Column(precision = 10, scale = 2)
	private BigDecimal largoCm = BigDecimal.ZERO;

	@Column(precision = 14, scale = 2)
	private BigDecimal volumenCm3 = BigDecimal.ZERO;

	@Column(precision = 14, scale = 2)
	private BigDecimal valorDeclarado = BigDecimal.ZERO;

	@Column(length = 80)
	private String categoriaAndreani;

	@PrePersist
	@PreUpdate
	public void calcularVolumen() {
		if (altoCm != null && anchoCm != null && largoCm != null) {
			volumenCm3 = altoCm.multiply(anchoCm).multiply(largoCm);
		}
		if (valorDeclarado == null || BigDecimal.ZERO.compareTo(valorDeclarado) == 0) {
			valorDeclarado = precio;
		}
	}

	public boolean tieneStockWeb(Integer cantidad) {
		return stockWeb != null && cantidad != null && stockWeb >= cantidad;
	}

	public boolean tieneStockFisico(Integer cantidad) {
		return stockFisico != null && cantidad != null && stockFisico >= cantidad;
	}
}
