package com.analistas.electrodental.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "andreani")
@Getter
@Setter
public class AndreaniProperties {

	private String apiUrl = "https://apisqa.andreani.com";
	private String token;
	private String contrato;
	private String cliente;
	private String sucursalOrigen;
	private String cpOrigen;
	private String ciudadOrigen;
	private String paisOrigen = "AR";
	private String paisDestino = "AR";
	private String categoriaDefault = "Otros";
}
