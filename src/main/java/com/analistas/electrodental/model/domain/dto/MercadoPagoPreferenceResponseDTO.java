package com.analistas.electrodental.model.domain.dto;

public record MercadoPagoPreferenceResponseDTO(
		String preferenceId,
		String externalReference,
		String initPoint,
		String sandboxInitPoint,
		String requestJson,
		String responseJson) {
}
