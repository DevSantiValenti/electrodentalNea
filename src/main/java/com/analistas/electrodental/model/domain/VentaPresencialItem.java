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
@Table(name = "venta_presencial_items")
@Getter
@Setter
@NoArgsConstructor
public class VentaPresencialItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venta_presencial_id", nullable = false)
	private VentaPresencial ventaPresencial;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id", nullable = false)
	private Producto producto;

	@Column(nullable = false)
	private Integer cantidad = 1;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal precioUnitarioSnapshot = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal subtotal = BigDecimal.ZERO;

	@PrePersist
	@PreUpdate
	public void calcularSubtotal() {
		if (precioUnitarioSnapshot != null && cantidad != null) {
			subtotal = precioUnitarioSnapshot.multiply(BigDecimal.valueOf(cantidad));
		}
	}
}
