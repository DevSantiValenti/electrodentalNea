package com.analistas.electrodental.model.domain.dto;

public record ResultadoDescuentoDTO(
		boolean valido,
		String mensaje,
		CarritoDTO carrito) {
}
