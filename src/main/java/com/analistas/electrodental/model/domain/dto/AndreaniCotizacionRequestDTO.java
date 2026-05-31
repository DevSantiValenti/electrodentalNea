package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record AndreaniCotizacionRequestDTO(
		String codigoPostalDestino,
		String contrato,
		String clienteAndreani,
		String sucursalOrigen,
		BigDecimal valorDeclarado,
		BigDecimal pesoTotalKg,
		BigDecimal volumenTotalCm3,
		BigDecimal altoMaxCm,
		BigDecimal anchoMaxCm,
		BigDecimal largoMaxCm,
		String categoriaAndreani) {
}
