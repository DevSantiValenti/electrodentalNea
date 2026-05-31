package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record CarritoDTO(
		List<CarritoItemDTO> items,
		BigDecimal subtotal,
		Integer cantidadTotal) {

	public CarritoDTO() {
		this(new ArrayList<>(), BigDecimal.ZERO, 0);
	}
}
