package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record CarritoItemDTO(
		Long productoId,
		String slug,
		String nombre,
		String imagenPrincipal,
		BigDecimal precioUnitario,
		Integer cantidad,
		BigDecimal subtotal,
		Integer stockDisponible,
		Boolean envioOcaDesactivado,
		Boolean oferta,
		BigDecimal descuentoAplicado,
		BigDecimal totalConDescuento) {

	public CarritoItemDTO(
			Long productoId,
			String slug,
			String nombre,
			String imagenPrincipal,
			BigDecimal precioUnitario,
			Integer cantidad,
			BigDecimal subtotal,
			Integer stockDisponible,
			Boolean envioOcaDesactivado) {
		this(
				productoId,
				slug,
				nombre,
				imagenPrincipal,
				precioUnitario,
				cantidad,
				subtotal,
				stockDisponible,
				envioOcaDesactivado,
				false);
	}

	public CarritoItemDTO(
			Long productoId,
			String slug,
			String nombre,
			String imagenPrincipal,
			BigDecimal precioUnitario,
			Integer cantidad,
			BigDecimal subtotal,
			Integer stockDisponible,
			Boolean envioOcaDesactivado,
			Boolean oferta) {
		this(
				productoId,
				slug,
				nombre,
				imagenPrincipal,
				precioUnitario,
				cantidad,
				subtotal,
				stockDisponible,
				envioOcaDesactivado,
				oferta,
				BigDecimal.ZERO,
				subtotal);
	}

	public boolean tieneOferta() {
		return Boolean.TRUE.equals(oferta);
	}

	public BigDecimal descuentoAplicado() {
		return descuentoAplicado == null ? BigDecimal.ZERO : descuentoAplicado;
	}

	public BigDecimal totalConDescuento() {
		return totalConDescuento == null ? subtotal : totalConDescuento;
	}

	public boolean tieneDescuento() {
		return descuentoAplicado().signum() > 0;
	}

	public CarritoItemDTO conDescuento(BigDecimal descuento) {
		BigDecimal descuentoSeguro = descuento == null ? BigDecimal.ZERO : descuento;
		BigDecimal total = subtotal.subtract(descuentoSeguro);
		if (total.signum() < 0) {
			total = BigDecimal.ZERO;
		}
		return new CarritoItemDTO(
				productoId,
				slug,
				nombre,
				imagenPrincipal,
				precioUnitario,
				cantidad,
				subtotal,
				stockDisponible,
				envioOcaDesactivado,
				oferta,
				descuentoSeguro,
				total);
	}

	public CarritoItemDTO sinDescuento() {
		return conDescuento(BigDecimal.ZERO);
	}
}
