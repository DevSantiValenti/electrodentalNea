package com.analistas.electrodental.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf((csrf) -> csrf.disable())
				.authorizeHttpRequests((requests) -> requests
				.requestMatchers(
						"/",
						"/inicio",
						"/catalogo",
						"/productos",
						"/productos/**",
						"/producto",
						"/producto/**",
						"/carrito",
						"/carrito/**",
						"/checkout",
						"/checkout/**",
						"/finalizar-compra",
						"/ofertas",
						"/marcas",
						"/contacto",
						"/admin",
						"/admin/**",
						"/panel",
						"/api/mercadopago/webhook",
						"/servicio-tecnico",
						"/denttech",
						"/css/**",
						"/img/**",
						"/js/**",
						"/images/**")
				.permitAll()
				.anyRequest()
				.authenticated());

		return http.build();
	}
}
