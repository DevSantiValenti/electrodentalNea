package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record AndreaniCotizacionResponseDTO(
		boolean cotizada,
		String proveedor,
		BigDecimal costo,
		String moneda,
		String mensaje,
		String requestJson,
		String responseJson) {
}
