package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record CarritoItemDTO(
		Long productoId,
		String slug,
		String nombre,
		String imagenPrincipal,
		BigDecimal precioUnitario,
		Integer cantidad,
		BigDecimal subtotal,
		Integer stockDisponible) {
}
