package com.analistas.electrodental.web.controller;

import java.math.BigDecimal;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** Builds SEO metadata only; never redirects requests or changes stored slugs. */
final class SeoUrls {

	private static final String ORIGIN = "https://electrodentalnea.com.ar";

	private SeoUrls() {
	}

	static String canonical(String path, String... segments) {
		return UriComponentsBuilder.fromUriString(ORIGIN).path(path).pathSegment(segments)
				.build().encode().toUriString();
	}

	static String catalogo(String categoria, String subcategoria, String q, String marca,
			BigDecimal precioMin, BigDecimal precioMax) {
		var uri = UriComponentsBuilder.fromUriString(ORIGIN).path("/catalogo");
		boolean filtrado = StringUtils.hasText(q) || StringUtils.hasText(marca) || precioMin != null || precioMax != null;
		// Category path handlers do not accept search/price filters, so keep the query route for those combinations.
		if (StringUtils.hasText(subcategoria)) {
			if (filtrado) uri.queryParam("subcategoria", subcategoria);
			else uri.pathSegment("subcategoria", subcategoria);
		} else if (StringUtils.hasText(categoria)) {
			if (filtrado) uri.queryParam("categoria", categoria);
			else uri.pathSegment("categoria", categoria);
		}
		// Keep content-changing filters; tracking parameters are not part of the canonical URL.
		if (StringUtils.hasText(q)) uri.queryParam("q", q);
		if (StringUtils.hasText(marca)) uri.queryParam("marca", marca);
		if (precioMin != null) uri.queryParam("precioMin", precioMin.stripTrailingZeros().toPlainString());
		if (precioMax != null) uri.queryParam("precioMax", precioMax.stripTrailingZeros().toPlainString());
		// A literal plus must not become a space when Spring reads query parameters.
		return uri.build().encode().toUriString().replace("+", "%2B");
	}
}
