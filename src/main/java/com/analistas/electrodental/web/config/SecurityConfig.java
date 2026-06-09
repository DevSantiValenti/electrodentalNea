package com.analistas.electrodental.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;

@Configuration
public class SecurityConfig {

	private static final String DEFAULT_ADMIN_USER = "admin";
	private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

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
						"/api/productos/buscar",
						"/api/mercadopago/webhook",
						"/servicio-tecnico",
						"/denttech",
						"/css/**",
						"/img/**",
						"/js/**",
						"/images/**")
				.permitAll()
				.requestMatchers("/admin/login").permitAll()
				.requestMatchers("/admin", "/admin/**", "/panel", "/api/clientes/buscar").hasRole("ADMIN")
				.anyRequest()
				.authenticated())
				.formLogin((form) -> form
						.loginPage("/admin/login")
						.loginProcessingUrl("/admin/login")
						.defaultSuccessUrl("/admin", true)
						.failureUrl("/admin/login?error")
						.permitAll())
				.logout((logout) -> logout
						.logoutUrl("/admin/logout")
						.logoutSuccessUrl("/admin/login?logout")
						.permitAll());

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(
			IConfiguracionTiendaService configuracionTiendaService,
			PasswordEncoder passwordEncoder) {
		return username -> {
			ConfiguracionTienda configuracion = configuracionTiendaService.obtener();
			String adminUsuario = StringUtils.hasText(configuracion.getAdminUsuario())
					? configuracion.getAdminUsuario().trim()
					: DEFAULT_ADMIN_USER;
			if (!adminUsuario.equalsIgnoreCase(username)) {
				throw new UsernameNotFoundException("Usuario admin no encontrado");
			}
			String passwordHash = StringUtils.hasText(configuracion.getAdminPasswordHash())
					? configuracion.getAdminPasswordHash()
					: passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD);
			return User.withUsername(adminUsuario)
					.password(passwordHash)
					.roles("ADMIN")
					.build();
		};
	}
}
