package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record CarritoDTO(
		List<CarritoItemDTO> items,
		BigDecimal subtotal,
		Integer cantidadTotal,
		DescuentoAplicadoDTO descuento) {

	public CarritoDTO() {
		this(new ArrayList<>(), BigDecimal.ZERO, 0, null);
	}

	public CarritoDTO(List<CarritoItemDTO> items, BigDecimal subtotal, Integer cantidadTotal) {
		this(items, subtotal, cantidadTotal, null);
	}

	public BigDecimal descuentoTotal() {
		return descuento == null || descuento.monto() == null ? BigDecimal.ZERO : descuento.monto();
	}

	public BigDecimal total() {
		BigDecimal total = (subtotal == null ? BigDecimal.ZERO : subtotal).subtract(descuentoTotal());
		return total.signum() < 0 ? BigDecimal.ZERO : total;
	}

	public boolean tieneDescuento() {
		return descuento != null && descuentoTotal().signum() > 0;
	}

	public CarritoDTO conDescuento(DescuentoAplicadoDTO descuento) {
		return new CarritoDTO(items, subtotal, cantidadTotal, descuento);
	}

	public CarritoDTO sinDescuento() {
		return new CarritoDTO(
				items.stream()
						.map(CarritoItemDTO::sinDescuento)
						.toList(),
				subtotal,
				cantidadTotal,
				null);
	}
}
