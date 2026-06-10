package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record OcaCotizacionResponseDTO(
		boolean cotizada,
		String proveedor,
		BigDecimal costo,
		String moneda,
		String mensaje,
		String plazoEntrega,
		String requestXml,
		String responseXml) {
}
