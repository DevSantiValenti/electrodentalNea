package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public record CarritoDTO(
		List<CarritoItemDTO> items,
		BigDecimal subtotal,
		Integer cantidadTotal,
		DescuentoAplicadoDTO descuento) {

	private static final BigDecimal DESCUENTO_TRANSFERENCIA = BigDecimal.valueOf(10);
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

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

	public BigDecimal descuentoTransferencia() {
		return total()
				.multiply(DESCUENTO_TRANSFERENCIA)
				.divide(CIEN, 2, RoundingMode.HALF_UP);
	}

	public BigDecimal totalTransferencia() {
		BigDecimal totalTransferencia = total().subtract(descuentoTransferencia());
		return totalTransferencia.signum() < 0 ? BigDecimal.ZERO : totalTransferencia;
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
