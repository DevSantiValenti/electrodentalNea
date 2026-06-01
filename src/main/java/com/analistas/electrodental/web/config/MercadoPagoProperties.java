package com.analistas.electrodental.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "mercadopago")
@Getter
@Setter
public class MercadoPagoProperties {

	private String apiUrl = "https://api.mercadopago.com";
	private boolean sandbox = true;
	private String accessToken;
	private String publicKey;
}
