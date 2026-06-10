package com.analistas.electrodental.model.domain.dto;

public record OcaCreacionEnvioResponseDTO(
		boolean creado,
		String numeroOrdenRetiro,
		String numeroEnvio,
		String mensaje,
		String requestXml,
		String responseXml) {
}
