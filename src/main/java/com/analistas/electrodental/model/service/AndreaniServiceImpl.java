package com.analistas.electrodental.model.service;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.AndreaniCotizacionRequestDTO;
import com.analistas.electrodental.model.domain.dto.AndreaniCotizacionResponseDTO;
import com.analistas.electrodental.web.config.AndreaniProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AndreaniServiceImpl implements IAndreaniService {

	private final AndreaniProperties properties;
	private final ObjectMapper objectMapper;

	public AndreaniServiceImpl(AndreaniProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public AndreaniCotizacionResponseDTO cotizar(Pedido pedido) {
		AndreaniCotizacionRequestDTO request = crearRequest(pedido);
		String requestJson = toJson(request);

		if (!configuracionCompleta()) {
			return new AndreaniCotizacionResponseDTO(
					false,
					"ANDREANI",
					BigDecimal.ZERO,
					"ARS",
					"Configura andreani.api-url, andreani.token, andreani.contrato y andreani.cliente para cotizar.",
					requestJson,
					null);
		}

		try {
			RestClient restClient = RestClient.create(properties.getApiUrl());
			JsonNode response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/v1/tarifas")
							.queryParam("cpDestino", request.codigoPostalDestino())
							.queryParam("contrato", request.contrato())
							.queryParam("cliente", request.clienteAndreani())
							.queryParamIfPresent("sucursalOrigen", optionalText(request.sucursalOrigen()))
							.queryParam("bultos[0][valorDeclarado]", request.valorDeclarado())
							.queryParam("bultos[0][volumen]", request.volumenTotalCm3())
							.queryParam("bultos[0][kilos]", request.pesoTotalKg())
							.queryParam("bultos[0][altoCm]", request.altoMaxCm())
							.queryParam("bultos[0][largoCm]", request.largoMaxCm())
							.queryParam("bultos[0][anchoCm]", request.anchoMaxCm())
							.build())
					.header("x-authorization-token", properties.getToken())
					.retrieve()
					.body(JsonNode.class);

			BigDecimal costo = extraerCosto(response);
			return new AndreaniCotizacionResponseDTO(true, "ANDREANI", costo, "ARS", "Cotizacion generada", requestJson, toJson(response));
		} catch (RestClientResponseException ex) {
			return new AndreaniCotizacionResponseDTO(false, "ANDREANI", BigDecimal.ZERO, "ARS", ex.getMessage(), requestJson, ex.getResponseBodyAsString());
		}
	}

	private AndreaniCotizacionRequestDTO crearRequest(Pedido pedido) {
		DireccionEnvio direccion = Objects.requireNonNull(pedido.getDireccionEnvio(), "El pedido no tiene direccion de envio");
		BigDecimal valorDeclarado = BigDecimal.ZERO;
		BigDecimal pesoTotal = BigDecimal.ZERO;
		BigDecimal volumenTotal = BigDecimal.ZERO;
		BigDecimal altoMax = BigDecimal.ZERO;
		BigDecimal anchoMax = BigDecimal.ZERO;
		BigDecimal largoMax = BigDecimal.ZERO;
		String categoria = properties.getCategoriaDefault();

		for (PedidoItem item : pedido.getItems()) {
			Producto producto = item.getProducto();
			BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
			valorDeclarado = valorDeclarado.add(producto.getValorDeclarado().multiply(cantidad));
			pesoTotal = pesoTotal.add(producto.getPesoKg().multiply(cantidad));
			volumenTotal = volumenTotal.add(producto.getVolumenCm3().multiply(cantidad));
			altoMax = altoMax.max(producto.getAltoCm());
			anchoMax = anchoMax.max(producto.getAnchoCm());
			largoMax = largoMax.max(producto.getLargoCm());
			if (StringUtils.hasText(producto.getCategoriaAndreani())) {
				categoria = producto.getCategoriaAndreani();
			}
		}

		return new AndreaniCotizacionRequestDTO(
				direccion.getCodigoPostal(),
				properties.getContrato(),
				properties.getCliente(),
				properties.getSucursalOrigen(),
				valorDeclarado,
				pesoTotal,
				volumenTotal,
				altoMax,
				anchoMax,
				largoMax,
				categoria);
	}

	private boolean configuracionCompleta() {
		return StringUtils.hasText(properties.getApiUrl())
				&& StringUtils.hasText(properties.getToken())
				&& StringUtils.hasText(properties.getContrato())
				&& StringUtils.hasText(properties.getCliente());
	}

	private BigDecimal extraerCosto(JsonNode response) {
		JsonNode totalConIva = response.path("tarifaConIva").path("total");
		if (!totalConIva.isMissingNode() && !totalConIva.isNull()) {
			return totalConIva.decimalValue();
		}
		return response.path("UltimaMilla").decimalValue();
	}

	private java.util.Optional<String> optionalText(String value) {
		return StringUtils.hasText(value) ? java.util.Optional.of(value) : java.util.Optional.empty();
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception ex) {
			return "{}";
		}
	}
}
