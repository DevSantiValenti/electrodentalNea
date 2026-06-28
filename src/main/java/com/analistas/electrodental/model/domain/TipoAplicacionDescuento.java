package com.analistas.electrodental.model.domain;

public enum TipoAplicacionDescuento {
	CARRITO("Todo el carrito"),
	PRODUCTO("Productos seleccionados"),
	CATEGORIA("Categorias seleccionadas"),
	SUBCATEGORIA("Subcategorias seleccionadas"),
	SELECCION_PERSONALIZADA("Productos, categorias y subcategorias");

	private final String etiqueta;

	TipoAplicacionDescuento(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}
}
