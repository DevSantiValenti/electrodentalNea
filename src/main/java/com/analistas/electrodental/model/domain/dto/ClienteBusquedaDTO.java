package com.analistas.electrodental.model.domain.dto;

public record ClienteBusquedaDTO(
		Long id,
		String dniCuit,
		String nombre,
		String apellidoRazonSocial,
		String email,
		String telefono,
		String nombreCompleto) {
}
