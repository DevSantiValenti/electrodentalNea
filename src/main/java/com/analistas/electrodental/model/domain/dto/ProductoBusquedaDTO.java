package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record ProductoBusquedaDTO(
		Long id,
		String nombre,
		String marca,
		String imagen,
		String url,
		BigDecimal precio,
		Integer stockFisico,
		Integer stockWeb) {
}
