package com.analistas.electrodental.web.view;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.service.ProductoImagenStorageService;

@Component
public class ImageUrlHelper {

	private final ProductoImagenStorageService productoImagenStorageService;

	public ImageUrlHelper(ProductoImagenStorageService productoImagenStorageService) {
		this.productoImagenStorageService = productoImagenStorageService;
	}

	public String card(String imageUrl, String fallbackUrl) {
		if (!StringUtils.hasText(imageUrl)) {
			return fallbackUrl;
		}
		try {
			String cardUrl = productoImagenStorageService.cardUrl(imageUrl);
			return StringUtils.hasText(cardUrl) ? cardUrl : fallbackUrl;
		} catch (RuntimeException ignored) {
			return imageUrl;
		}
	}
}
