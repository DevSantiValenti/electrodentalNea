package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record OcaCotizacionRequestDTO(
		String cuit,
		String operativa,
		String codigoPostalOrigen,
		String codigoPostalDestino,
		Integer cantidadPaquetes,
		BigDecimal valorDeclarado,
		BigDecimal pesoTotalKg,
		BigDecimal volumenTotalM3,
		BigDecimal altoMaxCm,
		BigDecimal anchoMaxCm,
		BigDecimal largoMaxCm) {
}
