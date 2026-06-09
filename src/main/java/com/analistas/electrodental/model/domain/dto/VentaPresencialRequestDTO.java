package com.analistas.electrodental.model.domain.dto;

import java.util.ArrayList;
import java.util.List;

import com.analistas.electrodental.model.domain.MetodoPagoVenta;

public record VentaPresencialRequestDTO(
		List<VentaPresencialItemRequestDTO> items,
		String clienteDniCuit,
		String clienteNombre,
		String clienteApellidoRazonSocial,
		String clienteEmail,
		String clienteTelefono,
		MetodoPagoVenta metodoPago,
		String usuarioAdmin,
		String observaciones) {

	public VentaPresencialRequestDTO {
		items = items == null ? new ArrayList<>() : items;
	}
}
