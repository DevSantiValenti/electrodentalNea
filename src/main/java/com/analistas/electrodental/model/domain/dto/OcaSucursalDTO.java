package com.analistas.electrodental.model.domain.dto;

import java.io.Serializable;

public record OcaSucursalDTO(
		String idCentroImposicion,
		String sigla,
		String nombre,
		String calle,
		String numero,
		String localidad,
		String codigoPostal,
		String provincia,
		String telefono,
		String tipoAgencia,
		String horarioAtencion,
		String latitud,
		String longitud,
		boolean admitePaquetes,
		boolean entregaPaquetes) implements Serializable {

	public String direccionResumen() {
		String calleNumero = (valor(calle) + " " + valor(numero)).trim();
		String localidadProvincia = (valor(localidad) + ", " + valor(provincia)).trim();
		return (calleNumero + " - " + localidadProvincia).replaceAll("^ - | - $", "").trim();
	}

	private static String valor(String texto) {
		return texto == null ? "" : texto.trim();
	}
}
