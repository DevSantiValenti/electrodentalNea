package com.analistas.electrodental.web.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.analistas.electrodental.model.service.IPedidoService;
import com.analistas.electrodental.web.config.MercadoPagoProperties;

import tools.jackson.databind.JsonNode;

@RestController
public class MercadoPagoWebhookController {

	private final IPedidoService pedidoService;
	private final MercadoPagoProperties mercadoPagoProperties;

	public MercadoPagoWebhookController(IPedidoService pedidoService, MercadoPagoProperties mercadoPagoProperties) {
		this.pedidoService = pedidoService;
		this.mercadoPagoProperties = mercadoPagoProperties;
	}

	@PostMapping("/api/mercadopago/webhook")
	public ResponseEntity<Void> recibirPost(
			@RequestParam(required = false, name = "external_reference") String externalReference,
			@RequestParam(required = false, name = "payment_id") String paymentId,
			@RequestParam(required = false) String status,
			@RequestBody(required = false) Map<String, Object> payload) {
		procesarNotificacion(externalReference, paymentId, status, payload);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/mercadopago/webhook")
	public ResponseEntity<Void> recibirGet(
			@RequestParam(required = false, name = "external_reference") String externalReference,
			@RequestParam(required = false, name = "payment_id") String paymentId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String type,
			@RequestParam(required = false, name = "data.id") String dataId) {
		procesarNotificacion(externalReference, paymentId == null ? dataId : paymentId, status,
				Map.of("type", type == null ? "" : type));
		return ResponseEntity.ok().build();
	}

	@SuppressWarnings("unchecked")
	private void procesarNotificacion(String externalReference, String paymentId, String status,
			Map<String, Object> payload) {
		String resolvedPaymentId = paymentId;
		if (!StringUtils.hasText(resolvedPaymentId) && payload != null) {
			Object data = payload.get("data");
			if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
				resolvedPaymentId = String.valueOf(dataMap.get("id"));
			}
		}

		if (StringUtils.hasText(resolvedPaymentId)) {
			JsonNode payment = obtenerPago(resolvedPaymentId);
			String resolvedReference = textOrNull(payment.path("external_reference"));
			String resolvedStatus = textOrNull(payment.path("status"));
			if (StringUtils.hasText(resolvedReference)) {
				pedidoService.actualizarPagoMercadoPago(resolvedReference, resolvedPaymentId, resolvedStatus);
			}
			return;
		}

		if (StringUtils.hasText(externalReference)) {
			pedidoService.actualizarPagoMercadoPago(externalReference, paymentId, status);
		}
	}

	private JsonNode obtenerPago(String paymentId) {
		return RestClient.create(mercadoPagoProperties.getBaseUrl())
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
