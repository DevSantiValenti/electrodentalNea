package com.analistas.electrodental.model.domain.dto;

import com.analistas.electrodental.model.domain.EstadoEnvio;

public record OcaSincronizacionEnvioResponseDTO(
		boolean actualizado,
		EstadoEnvio estadoAnterior,
		EstadoEnvio estadoNuevo,
		String mensaje,
		String responseXml) {
}
