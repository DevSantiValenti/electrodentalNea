package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
import jakarta.persistence.Transient;
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

	private Boolean compraHabilitada = true;

	private Boolean envioOcaDesactivado = false;

	private Boolean eliminado = false;

	@Column(precision = 5, scale = 2)
	private BigDecimal porcentajeOferta = BigDecimal.ZERO;

	@Transient
	private String ofertaInput;

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

	@PrePersist
	@PreUpdate
	public void calcularVolumen() {
		if (altoCm != null && anchoCm != null && largoCm != null) {
			volumenCm3 = altoCm.multiply(anchoCm).multiply(largoCm);
		}
	}

	public boolean tieneStockWeb(Integer cantidad) {
		return stockWeb != null && cantidad != null && stockWeb >= cantidad;
	}

	public boolean tieneStockFisico(Integer cantidad) {
		return stockFisico != null && cantidad != null && stockFisico >= cantidad;
	}

	public boolean permiteCompraWeb() {
		return !Boolean.FALSE.equals(compraHabilitada);
	}

	public boolean tieneStockWebDisponible() {
		return stockWeb != null && stockWeb > 0;
	}

	public boolean disponibleParaCompraWeb() {
		return permiteCompraWeb() && tieneStockWebDisponible();
	}

	public boolean tieneOferta() {
		return Boolean.TRUE.equals(oferta);
	}

	public boolean tieneDescuentoOferta() {
		return tieneOferta()
				&& porcentajeOferta != null
				&& porcentajeOferta.compareTo(BigDecimal.ZERO) > 0;
	}

	public BigDecimal precioOferta() {
		if (!tieneDescuentoOferta()) {
			return precio == null ? BigDecimal.ZERO : precio;
		}
		BigDecimal precioSeguro = precio == null ? BigDecimal.ZERO : precio;
		BigDecimal descuento = porcentajeOferta.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
		return precioSeguro.subtract(precioSeguro.multiply(descuento)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal precioTransferencia10Off() {
		return precioOferta()
				.multiply(BigDecimal.valueOf(90))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	public String porcentajeOfertaTexto() {
		if (porcentajeOferta == null) {
			return "";
		}
		return porcentajeOferta.stripTrailingZeros().toPlainString();
	}
}
