package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ventas_presenciales")
@Getter
@Setter
@NoArgsConstructor
public class VentaPresencial {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToMany(mappedBy = "ventaPresencial", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VentaPresencialItem> items = new ArrayList<>();

	@ManyToOne
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal total = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private MetodoPagoVenta metodoPago = MetodoPagoVenta.EFECTIVO;

	@Column(length = 120)
	private String usuarioAdmin;

	private LocalDateTime fecha;

	@Column(columnDefinition = "TEXT")
	private String observaciones;

	@PrePersist
	public void prePersist() {
		fecha = fecha == null ? LocalDateTime.now() : fecha;
		recalcularTotal();
	}

	@PreUpdate
	public void preUpdate() {
		recalcularTotal();
	}

	public void agregarItem(VentaPresencialItem item) {
		item.setVentaPresencial(this);
		items.add(item);
		recalcularTotal();
	}

	public void recalcularTotal() {
		total = items.stream()
				.map(VentaPresencialItem::getSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
