package com.analistas.electrodental.model.domain.dto;

public record ProductoBusquedaDTO(
		Long id,
		String nombre,
		String marca,
		String imagen,
		String url) {
}
