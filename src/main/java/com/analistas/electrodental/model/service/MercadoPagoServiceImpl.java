package com.analistas.electrodental.model.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.analistas.electrodental.model.domain.Pago;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPreferenceResponseDTO;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.web.config.MercadoPagoProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MercadoPagoServiceImpl implements IMercadoPagoService {

	private final MercadoPagoProperties properties;
	private final IPagoRepository pagoRepository;
	private final ObjectMapper objectMapper;
	private final String appBaseUrl;

	public MercadoPagoServiceImpl(
			MercadoPagoProperties properties,
			IPagoRepository pagoRepository,
			ObjectMapper objectMapper,
			@Value("${app.base-url}") String appBaseUrl) {
		this.properties = properties;
		this.pagoRepository = pagoRepository;
		this.objectMapper = objectMapper;
		this.appBaseUrl = appBaseUrl;
	}

	@Override
	public MercadoPagoPreferenceResponseDTO crearPreferencia(Pedido pedido) {
		Pago pago = pedido.getPago();
		if (pago == null) {
			throw new IllegalArgumentException("El pedido no tiene pago asociado");
		}

		Map<String, Object> request = crearRequest(pedido, pago);
		String requestJson = toJson(request);

		if (!StringUtils.hasText(properties.getAccessToken())) {
			pago.setRequestPreference(requestJson);
			pago.setResponsePreference("{\"message\":\"Configura mercadopago.access-token para crear preferencias reales\"}");
			pagoRepository.save(pago);
			return new MercadoPagoPreferenceResponseDTO(null, pago.getExternalReference(), null, null, requestJson, pago.getResponsePreference());
		}

		try {
			JsonNode response = RestClient.create(properties.getApiUrl())
					.post()
					.uri("/checkout/preferences")
					.header("Authorization", "Bearer " + properties.getAccessToken())
					.body(request)
					.retrieve()
					.body(JsonNode.class);

			pago.setPreferenceId(response.path("id").asText(null));
			pago.setInitPoint(response.path("init_point").asText(null));
			pago.setSandboxInitPoint(response.path("sandbox_init_point").asText(null));
			pago.setRequestPreference(requestJson);
			pago.setResponsePreference(toJson(response));
			pagoRepository.save(pago);

			return new MercadoPagoPreferenceResponseDTO(
					pago.getPreferenceId(),
					pago.getExternalReference(),
					pago.getInitPoint(),
					pago.getSandboxInitPoint(),
					requestJson,
					pago.getResponsePreference());
		} catch (RestClientResponseException ex) {
			pago.setRequestPreference(requestJson);
			pago.setResponsePreference(ex.getResponseBodyAsString());
			pagoRepository.save(pago);
			throw ex;
		}
	}

	private Map<String, Object> crearRequest(Pedido pedido, Pago pago) {
		Map<String, Object> request = new LinkedHashMap<>();
		List<Map<String, Object>> items = pedido.getItems().stream()
				.map(this::crearItem)
				.toList();

		if (pedido.getCostoEnvio() != null && pedido.getCostoEnvio().signum() > 0) {
			items = new java.util.ArrayList<>(items);
			items.add(Map.of(
					"title", "Envio Andreani",
					"quantity", 1,
					"currency_id", "ARS",
					"unit_price", pedido.getCostoEnvio()));
		}

		request.put("items", items);
		request.put("external_reference", pago.getExternalReference());
		String baseUrl = normalizarBaseUrl(appBaseUrl);
		if (StringUtils.hasText(baseUrl)) {
			request.put("notification_url", baseUrl + "/api/mercadopago/webhook");
			Map<String, Object> backUrls = new LinkedHashMap<>();
			backUrls.put("success", baseUrl + "/checkout/mercadopago/success");
			backUrls.put("failure", baseUrl + "/checkout/mercadopago/failure");
			backUrls.put("pending", baseUrl + "/checkout/mercadopago/pending");
			request.put("back_urls", backUrls);
			request.put("auto_return", "approved");
		}
		return request;
	}

	private String normalizarBaseUrl(String baseUrl) {
		if (!StringUtils.hasText(baseUrl)) {
			return null;
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	private Map<String, Object> crearItem(PedidoItem item) {
		return Map.of(
				"id", String.valueOf(item.getProducto().getId()),
				"title", item.getNombreSnapshot(),
				"quantity", item.getCantidad(),
				"currency_id", "ARS",
				"unit_price", item.getPrecioUnitarioSnapshot());
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception ex) {
			return "{}";
		}
	}
}
