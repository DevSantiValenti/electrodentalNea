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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PedidoItem> items = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "direccion_envio_id")
	private DireccionEnvio direccionEnvio;

	@OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private Envio envio;

	@OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private Pago pago;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private CanalVenta canal = CanalVenta.WEB;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private EstadoPedido estadoPedido = EstadoPedido.BORRADOR;

	@Enumerated(EnumType.STRING)
	@Column(length = 40)
	private EstadoOperativoPedido estadoOperativo = EstadoOperativoPedido.N_A;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal subtotal = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal costoEnvio = BigDecimal.ZERO;

	@Column(precision = 14, scale = 2)
	private BigDecimal descuentoAplicado = BigDecimal.ZERO;

	@Column(precision = 14, scale = 2)
	private BigDecimal totalAntesDescuento = BigDecimal.ZERO;

	@Column(precision = 14, scale = 2)
	private BigDecimal totalDespuesDescuento = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal total = BigDecimal.ZERO;

	@Column(length = 60)
	private String codigoDescuento;

	@Column(length = 40)
	private String metodoEntrega;

	@Column(length = 40, unique = true)
	private String codigoCompra;

	private LocalDateTime fechaCreacion;

	private LocalDateTime fechaActualizacion;

	private LocalDateTime fechaPago;

	private LocalDateTime fechaCancelacion;

	@Transient
	private String advertenciaOperacion;

	@PrePersist
	public void prePersist() {
		fechaCreacion = LocalDateTime.now();
		fechaActualizacion = fechaCreacion;
		asegurarEstadoOperativo();
		recalcularTotales();
	}

	@PreUpdate
	public void preUpdate() {
		fechaActualizacion = LocalDateTime.now();
		asegurarEstadoOperativo();
		recalcularTotales();
	}

	public void agregarItem(PedidoItem item) {
		item.setPedido(this);
		items.add(item);
		recalcularTotales();
	}

	public void setEnvio(Envio envio) {
		this.envio = envio;
		if (envio != null) {
			envio.setPedido(this);
			costoEnvio = envio.getCostoCotizado() == null ? BigDecimal.ZERO : envio.getCostoCotizado();
		}
		recalcularTotales();
	}

	public void setPago(Pago pago) {
		this.pago = pago;
		if (pago != null) {
			pago.setPedido(this);
		}
	}

	public BigDecimal getDescuentoAplicado() {
		return descuentoAplicado == null ? BigDecimal.ZERO : descuentoAplicado;
	}

	public EstadoOperativoPedido getEstadoOperativo() {
		return estadoOperativo == null ? EstadoOperativoPedido.N_A : estadoOperativo;
	}

	public BigDecimal getTotalAntesDescuento() {
		if (totalAntesDescuento != null) {
			return totalAntesDescuento;
		}
		return (subtotal == null ? BigDecimal.ZERO : subtotal).add(costoEnvio == null ? BigDecimal.ZERO : costoEnvio);
	}

	public BigDecimal getTotalDespuesDescuento() {
		return totalDespuesDescuento == null ? total : totalDespuesDescuento;
	}

	public void recalcularTotales() {
		subtotal = items.stream()
				.map(PedidoItem::getSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal envio = costoEnvio == null ? BigDecimal.ZERO : costoEnvio;
		BigDecimal descuento = descuentoAplicado == null ? BigDecimal.ZERO : descuentoAplicado;
		if (descuento.compareTo(subtotal) > 0) {
			descuento = subtotal;
			descuentoAplicado = descuento;
		}
		totalAntesDescuento = subtotal.add(envio);
		total = totalAntesDescuento.subtract(descuento);
		if (total.signum() < 0) {
			total = BigDecimal.ZERO;
		}
		totalDespuesDescuento = total;
	}

	private void asegurarEstadoOperativo() {
		if (estadoOperativo == null) {
			estadoOperativo = EstadoOperativoPedido.N_A;
		}
	}
}
