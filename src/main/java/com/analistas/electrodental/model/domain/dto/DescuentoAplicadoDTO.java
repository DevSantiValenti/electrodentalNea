package com.analistas.electrodental.model.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.analistas.electrodental.model.domain.TipoAplicacionDescuento;

public record DescuentoAplicadoDTO(
		Long descuentoId,
		String codigo,
		BigDecimal porcentaje,
		TipoAplicacionDescuento tipoAplicacion,
		BigDecimal subtotalAplicable,
		BigDecimal monto,
		String descripcion) implements Serializable {
}
