package com.analistas.electrodental.model.domain;

public enum EstadoOperativoPedido {
	N_A("N/A", "bg-surface-soft text-muted border-outline"),
	PREPARADO("Preparado", "bg-sky-50 text-sky-800 border-sky-200"),
	DESPACHADO("Despachado", "bg-amber-50 text-amber-800 border-amber-200"),
	ENTREGADO("Entregado", "bg-green-50 text-green-800 border-green-200");

	private final String etiqueta;
	private final String cssClass;

	EstadoOperativoPedido(String etiqueta, String cssClass) {
		this.etiqueta = etiqueta;
		this.cssClass = cssClass;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public String getCssClass() {
		return cssClass;
	}
}
