package com.analistas.electrodental.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "direcciones_envio")
@Getter
@Setter
@NoArgsConstructor
public class DireccionEnvio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	@Column(nullable = false, length = 160)
	private String calle;

	@Column(nullable = false, length = 30)
	private String numero;

	@Column(length = 80)
	private String pisoDepto;

	@Column(nullable = false, length = 120)
	private String ciudad;

	@Column(nullable = false, length = 120)
	private String provincia;

	@Column(nullable = false, length = 20)
	private String codigoPostal;

	@Column(nullable = false, length = 80)
	private String pais = "AR";
}
