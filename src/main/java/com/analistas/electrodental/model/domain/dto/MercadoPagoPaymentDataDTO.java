package com.analistas.electrodental.model.domain.dto;

import java.math.BigDecimal;

public record MercadoPagoPaymentDataDTO(
		String externalReference,
		String paymentId,
		String status,
		String statusDetail,
		String paymentMethodId,
		String paymentTypeId,
		BigDecimal transactionAmount) {
}
