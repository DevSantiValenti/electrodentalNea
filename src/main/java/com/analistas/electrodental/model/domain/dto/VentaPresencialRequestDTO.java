package com.analistas.electrodental.model.domain.dto;

import java.util.ArrayList;
import java.util.List;

import com.analistas.electrodental.model.domain.MetodoPagoVenta;

public record VentaPresencialRequestDTO(
		List<VentaPresencialItemRequestDTO> items,
		MetodoPagoVenta metodoPago,
		String usuarioAdmin,
		String observaciones) {

	public VentaPresencialRequestDTO {
		items = items == null ? new ArrayList<>() : items;
	}
}
