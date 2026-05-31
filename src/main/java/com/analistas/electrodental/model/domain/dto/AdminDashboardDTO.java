package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record AdminDashboardDTO(
		long pedidosHoy,
		long pedidosPendientesPago,
		long ventasPresencialesHoy,
		int productosBajoStock,
		BigDecimal ventasDelMes) {
}
