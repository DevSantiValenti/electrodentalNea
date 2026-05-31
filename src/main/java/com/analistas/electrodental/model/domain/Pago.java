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
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pedido_id", nullable = false, unique = true)
	private Pedido pedido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ProveedorPago proveedor = ProveedorPago.MERCADO_PAGO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private EstadoPago estadoPago = EstadoPago.PENDIENTE;

	@Column(length = 120)
	private String preferenceId;

	@Column(length = 120)
	private String paymentId;

	@Column(length = 120, unique = true)
	private String externalReference;

	@Column(length = 500)
	private String initPoint;

	@Column(length = 500)
	private String sandboxInitPoint;

	private LocalDateTime fechaAprobacion;

	@Lob
	private String requestPreference;

	@Lob
	private String responsePreference;
}
