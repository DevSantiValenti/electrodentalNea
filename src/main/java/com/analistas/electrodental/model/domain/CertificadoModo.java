package com.analistas.electrodental.model.domain;

public enum CertificadoModo {
	PUBLICO("Publico"),
	CODIGO("Con codigo");

	private final String etiqueta;

	CertificadoModo(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}
}
