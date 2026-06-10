package com.analistas.electrodental.model.service;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.analistas.electrodental.model.domain.EstadoPago;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPaymentDataDTO;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.web.config.MercadoPagoProperties;

import tools.jackson.databind.JsonNode;

@Service
public class MercadoPagoPendingPaymentPoller {

	private final IPagoRepository pagoRepository;
	private final IPedidoService pedidoService;
	private final MercadoPagoProperties mercadoPagoProperties;

	public MercadoPagoPendingPaymentPoller(
			IPagoRepository pagoRepository,
			IPedidoService pedidoService,
			MercadoPagoProperties mercadoPagoProperties) {
		this.pagoRepository = pagoRepository;
		this.pedidoService = pedidoService;
		this.mercadoPagoProperties = mercadoPagoProperties;
	}

	@Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
	public void revisarPagosPendientesRecientes() {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(10);
		pagoRepository.findPendientesRecientesConPaymentId(EstadoPago.PENDIENTE, desde)
				.forEach(pago -> {
					try {
						JsonNode payment = obtenerPago(pago.getPaymentId());
						MercadoPagoPaymentDataDTO paymentData = paymentData(payment, pago.getPaymentId());
						if (StringUtils.hasText(paymentData.status()) && StringUtils.hasText(paymentData.externalReference())) {
							pedidoService.actualizarPagoMercadoPago(paymentData);
						}
					} catch (RuntimeException ignored) {
						// Mercado Pago volvera a notificar; este poller solo cubre la ventana corta pending -> approved.
					}
				});
	}

	private MercadoPagoPaymentDataDTO paymentData(JsonNode payment, String fallbackPaymentId) {
		return new MercadoPagoPaymentDataDTO(
				textOrNull(payment.path("external_reference")),
				StringUtils.hasText(textOrNull(payment.path("id"))) ? textOrNull(payment.path("id")) : fallbackPaymentId,
				textOrNull(payment.path("status")),
				textOrNull(payment.path("status_detail")),
				textOrNull(payment.path("payment_method_id")),
				textOrNull(payment.path("payment_type_id")),
				decimalOrNull(payment.path("transaction_amount")));
	}

	private BigDecimal decimalOrNull(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		try {
			return new BigDecimal(node.asText());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private JsonNode obtenerPago(String paymentId) {
		return RestClient.create(mercadoPagoProperties.getApiUrl())
				.get()
				.uri("/v1/payments/{paymentId}", paymentId)
				.header("Authorization", "Bearer " + mercadoPagoProperties.getAccessToken())
				.retrieve()
				.body(JsonNode.class);
	}

	private String textOrNull(JsonNode node) {
		return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
	}
}
