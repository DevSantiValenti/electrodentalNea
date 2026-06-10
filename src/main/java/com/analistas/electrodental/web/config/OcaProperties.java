package com.analistas.electrodental.web.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "oca")
@Getter
@Setter
public class OcaProperties {

	private String apiUrl = "https://integraciones.ocadev.com.ar/epak_tracking_test/Oep_TrackEPak.asmx";
	private String usuario;
	private String password;
	private String cuit;
	private String numeroCuenta;
	private String operativa;
	private String operativaSucursal;
	private String codigoPostalOrigen = "3500";
	private String calleOrigen = "Roque Saenz Pena";
	private String numeroOrigen = "539";
	private String localidadOrigen = "Resistencia";
	private String provinciaOrigen = "Chaco";
	private String emailOrigen = "info@electrodentalnea.com";
	private String contactoOrigen = "ElectrodentalNea";
	private String centroCosto = "1";
	private String idFranjaHoraria = "1";
	private String idCentroImposicionOrigen = "0";
	private boolean confirmarRetiro = true;
	private boolean logisticaInversa = false;
	private BigDecimal pesoDefaultKg = new BigDecimal("1.000");
	private BigDecimal altoDefaultCm = new BigDecimal("10.00");
	private BigDecimal anchoDefaultCm = new BigDecimal("10.00");
	private BigDecimal largoDefaultCm = new BigDecimal("10.00");
}
