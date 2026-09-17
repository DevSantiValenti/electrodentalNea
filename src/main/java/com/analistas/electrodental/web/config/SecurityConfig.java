package com.analistas.electrodental.web.config;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.repository.ICursoPanelUsuarioRepository;
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
						"/catalogo/**",
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
						"/cursos",
						"/cursos/**",
						"/capacitaciones",
						"/capacitaciones/**",
						"/marcas",
						"/contacto",
						"/.well-known/appspecific/com.chrome.devtools.json",
						"/robots.txt",
						"/sitemap.xml",
						"/api/productos/buscar",
						"/api/oca/**",
						"/api/mercadopago/webhook",
						"/servicio-tecnico",
						"/denttech",
						"/css/**",
						"/img/**",
						"/js/**",
						"/images/**",
						"/uploads/**")
				.permitAll()
				.requestMatchers("/admin/login", "/admin/cursos-panel/login").permitAll()
				.requestMatchers("/admin/cursos-panel", "/admin/cursos-panel/**").hasAnyRole("ADMIN", "CURSOS_ADMIN")
				.requestMatchers("/admin", "/admin/**", "/panel", "/api/clientes/buscar").hasRole("ADMIN")
				.anyRequest()
				.authenticated())
				.exceptionHandling((exceptions) -> exceptions
						.authenticationEntryPoint((request, response, authException) -> {
							String destino = esRutaCursosPanel(request.getRequestURI())
									? "/admin/cursos-panel/login"
									: "/admin/login";
							response.sendRedirect(request.getContextPath() + destino);
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							if (tieneRol(request.getUserPrincipal() instanceof Authentication authentication ? authentication : null, "ROLE_CURSOS_ADMIN")) {
								response.sendRedirect(request.getContextPath() + "/admin/cursos-panel?denied");
								return;
							}
							response.sendRedirect(request.getContextPath() + "/admin?denied");
						}))
				.formLogin((form) -> form
						.loginPage("/admin/login")
						.loginProcessingUrl("/admin/login")
						.successHandler(adminAuthenticationSuccessHandler())
						.failureHandler(adminAuthenticationFailureHandler())
						.permitAll())
				.logout((logout) -> logout
						.logoutUrl("/admin/logout")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID")
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
			PasswordEncoder passwordEncoder,
			ICursoPanelUsuarioRepository cursoPanelUsuarioRepository) {
		return username -> {
			ConfiguracionTienda configuracion = configuracionTiendaService.obtener();
			String adminUsuario = StringUtils.hasText(configuracion.getAdminUsuario())
					? configuracion.getAdminUsuario().trim()
					: DEFAULT_ADMIN_USER;
			if (adminUsuario.equalsIgnoreCase(username)) {
				String passwordHash = StringUtils.hasText(configuracion.getAdminPasswordHash())
						? configuracion.getAdminPasswordHash()
						: passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD);
				return User.withUsername(adminUsuario)
						.password(passwordHash)
						.roles("ADMIN")
						.build();
			}
			return cursoPanelUsuarioRepository.findByUsuarioIgnoreCase(username)
					.filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
					.map(usuario -> User.withUsername(usuario.getUsuario())
							.password(usuario.getPasswordHash())
							.roles("CURSOS_ADMIN")
							.build())
					.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
		};
	}

	private AuthenticationSuccessHandler adminAuthenticationSuccessHandler() {
		HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
		return (request, response, authentication) -> {
			SavedRequest savedRequest = requestCache.getRequest(request, response);
			if (savedRequest != null && puedeUsarDestinoGuardado(savedRequest, authentication)) {
				response.sendRedirect(savedRequest.getRedirectUrl());
				return;
			}
			String referer = request.getHeader(HttpHeaders.REFERER);
			if (referer != null && referer.contains("/admin/cursos-panel/login")) {
				response.sendRedirect(request.getContextPath() + "/admin/cursos-panel");
				return;
			}
			String destino = tieneRol(authentication, "ROLE_CURSOS_ADMIN") && !tieneRol(authentication, "ROLE_ADMIN")
					? "/admin/cursos-panel"
					: "/admin";
			response.sendRedirect(request.getContextPath() + destino);
		};
	}

	private AuthenticationFailureHandler adminAuthenticationFailureHandler() {
		return (request, response, exception) -> {
			String referer = request.getHeader(HttpHeaders.REFERER);
			String destino = referer != null && referer.contains("/admin/cursos-panel/login")
					? "/admin/cursos-panel/login?error"
					: "/admin/login?error";
			response.sendRedirect(request.getContextPath() + destino);
		};
	}

	private boolean puedeUsarDestinoGuardado(SavedRequest savedRequest, Authentication authentication) {
		if (tieneRol(authentication, "ROLE_ADMIN")) {
			return true;
		}
		if (!tieneRol(authentication, "ROLE_CURSOS_ADMIN")) {
			return false;
		}
		try {
			return esRutaCursosPanel(URI.create(savedRequest.getRedirectUrl()).getPath());
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static boolean esRutaCursosPanel(String path) {
		return path != null && (path.equals("/admin/cursos-panel") || path.startsWith("/admin/cursos-panel/"));
	}

	private static boolean tieneRol(Authentication authentication, String rol) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> rol.equals(authority.getAuthority()));
	}
}
