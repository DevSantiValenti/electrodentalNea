package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.analistas.electrodental.model.domain.dto.MercadoPagoPaymentDataDTO;
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
			@RequestHeader(required = false, name = "x-signature") String signature,
			@RequestHeader(required = false, name = "x-request-id") String requestId,
			@RequestBody(required = false) Map<String, Object> payload) {
		if (!firmaWebhookValida(signature, requestId, resolverDataId(paymentId, payload))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			procesarNotificacion(externalReference, paymentId, status, payload);
		} catch (RuntimeException ignored) {
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/mercadopago/webhook")
	public ResponseEntity<Void> recibirGet(
			@RequestParam(required = false, name = "external_reference") String externalReference,
			@RequestParam(required = false, name = "payment_id") String paymentId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String type,
			@RequestParam(required = false, name = "data.id") String dataId,
			@RequestHeader(required = false, name = "x-signature") String signature,
			@RequestHeader(required = false, name = "x-request-id") String requestId) {
		String resolvedPaymentId = paymentId == null ? dataId : paymentId;
		if (!firmaWebhookValida(signature, requestId, resolvedPaymentId)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			procesarNotificacion(externalReference, resolvedPaymentId, status,
					Map.of("type", type == null ? "" : type));
		} catch (RuntimeException ignored) {
		}
		return ResponseEntity.ok().build();
	}

	@SuppressWarnings("unchecked")
	private void procesarNotificacion(String externalReference, String paymentId, String status,
			Map<String, Object> payload) {
		if (payload != null) {
			Object eventType = payload.get("type");
			Object topic = payload.get("topic");
			String tipoNotificacion = eventType == null ? String.valueOf(topic) : String.valueOf(eventType);
			if (StringUtils.hasText(tipoNotificacion) && !"payment".equalsIgnoreCase(tipoNotificacion)) {
				return;
			}
		}

		String resolvedPaymentId = paymentId;
		if (!StringUtils.hasText(resolvedPaymentId) && payload != null) {
			Object data = payload.get("data");
			if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
				resolvedPaymentId = String.valueOf(dataMap.get("id"));
			}
		}

		if (StringUtils.hasText(resolvedPaymentId)) {
			JsonNode payment = obtenerPago(resolvedPaymentId);
			MercadoPagoPaymentDataDTO paymentData = paymentData(payment, resolvedPaymentId);
			if (StringUtils.hasText(paymentData.externalReference())) {
				pedidoService.actualizarPagoMercadoPago(paymentData);
			}
			return;
		}

		if (StringUtils.hasText(externalReference)) {
			pedidoService.actualizarPagoMercadoPago(externalReference, paymentId, status);
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

	private String textOrNull(JsonNode node) {
		return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
	}

	private String resolverDataId(String paymentId, Map<String, Object> payload) {
		if (StringUtils.hasText(paymentId)) {
			return paymentId;
		}
		if (payload != null) {
			Object data = payload.get("data");
			if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
				return String.valueOf(dataMap.get("id"));
			}
		}
		return null;
	}

	private boolean firmaWebhookValida(String signature, String requestId, String dataId) {
		String secret = mercadoPagoProperties.getWebhookSecret();
		if (!StringUtils.hasText(secret)) {
			return true;
		}
		if (!StringUtils.hasText(signature) || !StringUtils.hasText(requestId) || !StringUtils.hasText(dataId)) {
			return false;
		}
		Map<String, String> partes = parseSignature(signature);
		String ts = partes.get("ts");
		String v1 = partes.get("v1");
		if (!StringUtils.hasText(ts) || !StringUtils.hasText(v1)) {
			return false;
		}
		String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
		String expected = hmacSha256Hex(manifest, secret);
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
	}

	private Map<String, String> parseSignature(String signature) {
		Map<String, String> partes = new HashMap<>();
		Arrays.stream(signature.split(","))
				.map(String::trim)
				.map(part -> part.split("=", 2))
				.filter(part -> part.length == 2)
				.forEach(part -> partes.put(part[0], part[1]));
		return partes;
	}

	private String hmacSha256Hex(String data, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo validar la firma de Mercado Pago", ex);
		}
	}
}
