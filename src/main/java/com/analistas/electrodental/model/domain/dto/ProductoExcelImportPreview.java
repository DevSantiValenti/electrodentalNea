package com.analistas.electrodental.model.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record ProductoExcelImportPreview(
		int filasLeidas,
		List<Update> actualizaciones,
		List<Deletion> eliminaciones,
		List<String> errores,
		boolean importacionParcial) implements Serializable {

	public ProductoExcelImportPreview(
			int filasLeidas,
			List<Update> actualizaciones,
			List<Deletion> eliminaciones,
			List<String> errores) {
		this(filasLeidas, actualizaciones, eliminaciones, errores, false);
	}

	public boolean tieneCambios() {
		return !actualizaciones.isEmpty() || !eliminaciones.isEmpty();
	}

	public boolean puedeConfirmar() {
		return errores.isEmpty() && tieneCambios();
	}

	public int totalCambios() {
		return actualizaciones.size() + eliminaciones.size();
	}

	public record Update(
			Long productoId,
			String categoria,
			String subcategoria,
			String nombreActual,
			String nombreNuevo,
			String marcaNueva,
			Integer stockWebNuevo,
			Integer stockFisicoNuevo,
			BigDecimal precioNuevo,
			List<FieldChange> cambios) implements Serializable {
	}

	public record FieldChange(
			String campo,
			String actual,
			String nuevo) implements Serializable {
	}

	public record Deletion(
			Long productoId,
			String categoria,
			String subcategoria,
			String nombre) implements Serializable {
	}
}
