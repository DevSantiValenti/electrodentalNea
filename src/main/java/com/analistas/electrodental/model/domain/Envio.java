package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;
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
@Table(name = "envios")
@Getter
@Setter
@NoArgsConstructor
public class Envio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pedido_id", nullable = false, unique = true)
	private Pedido pedido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ProveedorEnvio proveedor = ProveedorEnvio.ANDREANI;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private EstadoEnvio estadoEnvio = EstadoEnvio.SIN_COTIZAR;

	@Column(length = 20)
	private String codigoPostalOrigen;

	@Column(length = 120)
	private String ciudadOrigen;

	@Column(length = 20)
	private String codigoPostalDestino;

	@Column(length = 120)
	private String ciudadDestino;

	@Column(length = 80)
	private String contrato;

	@Column(length = 80)
	private String clienteAndreani;

	@Column(precision = 14, scale = 2)
	private BigDecimal costoCotizado = BigDecimal.ZERO;

	@Column(precision = 10, scale = 3)
	private BigDecimal pesoTotalKg = BigDecimal.ZERO;

	@Column(precision = 14, scale = 2)
	private BigDecimal volumenTotalCm3 = BigDecimal.ZERO;

	@Column(precision = 10, scale = 2)
	private BigDecimal altoMaxCm = BigDecimal.ZERO;

	@Column(precision = 10, scale = 2)
	private BigDecimal anchoMaxCm = BigDecimal.ZERO;

	@Column(precision = 10, scale = 2)
	private BigDecimal largoMaxCm = BigDecimal.ZERO;

	@Column(length = 80)
	private String categoriaAndreani;

	@Column(length = 120)
	private String tracking;

	private LocalDateTime fechaCotizacion;

	@Lob
	private String requestCotizacion;

	@Lob
	private String responseCotizacion;
}
