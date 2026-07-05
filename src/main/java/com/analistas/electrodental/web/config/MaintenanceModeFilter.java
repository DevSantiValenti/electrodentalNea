package com.analistas.electrodental.web.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.analistas.electrodental.model.service.IConfiguracionTiendaService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

	private static final List<String> ADMIN_PATHS = List.of(
			"/admin",
			"/panel",
			"/api/clientes/buscar");

	private static final String MAINTENANCE_LOGO_PATH = "/img/electrodentallarge.png";

	private final IConfiguracionTiendaService configuracionTiendaService;

	public MaintenanceModeFilter(IConfiguracionTiendaService configuracionTiendaService) {
		this.configuracionTiendaService = configuracionTiendaService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String path = normalizarPath(request);
		if (!esRutaAdmin(path) && !esLogoMantenimiento(path) && configuracionTiendaService.obtener().paginaOcultaActiva()) {
			mostrarMantenimiento(response);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private String normalizarPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		return path.isBlank() ? "/" : path;
	}

	private boolean esRutaAdmin(String path) {
		return ADMIN_PATHS.stream()
				.anyMatch(adminPath -> path.equals(adminPath) || path.startsWith(adminPath + "/"));
	}

	private boolean esLogoMantenimiento(String path) {
		return MAINTENANCE_LOGO_PATH.equals(path);
	}

	private void mostrarMantenimiento(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/html;charset=UTF-8");
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader(HttpHeaders.PRAGMA, "no-cache");
		response.getWriter().write(PAGINA_MANTENIMIENTO);
	}

	private static final String PAGINA_MANTENIMIENTO = """
			<!DOCTYPE html>
			<html lang="es">
			<head>
				<meta charset="utf-8">
				<meta name="viewport" content="width=device-width, initial-scale=1">
				<title>Estamos en mantenimiento | ElectrodentalNea</title>
				<style>
					:root {
						--ink: #111827;
						--muted: #5b6776;
						--brand: #b71f2a;
						--line: #d8dee8;
						--surface: #ffffff;
						--bg: #f6f8fb;
					}
					* { box-sizing: border-box; }
					body {
						margin: 0;
						min-height: 100vh;
						display: grid;
						place-items: center;
						background: var(--bg);
						color: var(--ink);
						font-family: Arial, Helvetica, sans-serif;
					}
					main {
						width: min(92vw, 1120px);
						padding: 32px 24px;
						text-align: center;
					}
					.logo-wrap {
						display: flex;
						align-items: center;
						justify-content: center;
						width: min(100%, 1100px);
						min-height: clamp(130px, 20vw, 190px);
						margin: 0 auto 18px;
					}
					.logo {
						width: min(100%, 880px);
						max-height: clamp(100px, 16vw, 170px);
						object-fit: contain;
					}
					.scene {
						position: relative;
						width: min(260px, 74vw);
						height: 190px;
						margin: 0 auto 26px;
					}
					.monitor {
						position: absolute;
						left: 50%;
						bottom: 18px;
						width: 210px;
						height: 128px;
						transform: translateX(-50%);
						border: 5px solid var(--ink);
						border-radius: 14px;
						background: var(--surface);
						box-shadow: 0 12px 0 #e5e9f0;
					}
					.monitor::before {
						content: "";
						position: absolute;
						left: 50%;
						bottom: -34px;
						width: 56px;
						height: 30px;
						transform: translateX(-50%);
						border-radius: 0 0 8px 8px;
						background: var(--ink);
					}
					.monitor::after {
						content: "";
						position: absolute;
						left: 50%;
						bottom: -43px;
						width: 108px;
						height: 10px;
						transform: translateX(-50%);
						border-radius: 99px;
						background: var(--ink);
					}
					.gear {
						position: absolute;
						top: 36px;
						left: 50%;
						width: 52px;
						height: 52px;
						margin-left: -26px;
						border: 9px dashed var(--brand);
						border-radius: 50%;
						animation: spin 2.2s linear infinite;
					}
					.worker {
						position: absolute;
						right: 16px;
						bottom: 42px;
						width: 62px;
						height: 84px;
					}
					.head {
						position: absolute;
						top: 0;
						left: 18px;
						width: 30px;
						height: 30px;
						border-radius: 50%;
						background: #ffd8b5;
						border: 3px solid var(--ink);
					}
					.helmet {
						position: absolute;
						top: -7px;
						left: 12px;
						width: 42px;
						height: 20px;
						border-radius: 22px 22px 8px 8px;
						background: #f6c343;
						border: 3px solid var(--ink);
					}
					.body {
						position: absolute;
						top: 30px;
						left: 15px;
						width: 36px;
						height: 42px;
						border-radius: 8px;
						background: var(--brand);
						border: 3px solid var(--ink);
					}
					.arm {
						position: absolute;
						top: 34px;
						left: -2px;
						width: 44px;
						height: 8px;
						border-radius: 99px;
						background: #ffd8b5;
						border: 3px solid var(--ink);
						transform-origin: right center;
						animation: hammer 1s ease-in-out infinite;
					}
					.tool {
						position: absolute;
						top: 23px;
						left: -13px;
						width: 10px;
						height: 34px;
						border-radius: 4px;
						background: var(--ink);
						transform: rotate(25deg);
					}
					h1 {
						margin: 0;
						font-size: clamp(1.9rem, 7vw, 3.1rem);
						line-height: 1.05;
						letter-spacing: 0;
					}
					p {
						max-width: 540px;
						margin: 16px auto 0;
						color: var(--muted);
						font-size: 1.08rem;
						font-weight: 700;
					}
					.badge {
						display: inline-flex;
						margin-bottom: 14px;
						padding: 8px 12px;
						border: 1px solid var(--line);
						border-radius: 999px;
						background: var(--surface);
						color: var(--brand);
						font-size: .82rem;
						font-weight: 800;
						text-transform: uppercase;
					}
					@keyframes spin {
						to { transform: rotate(360deg); }
					}
					@keyframes hammer {
						0%, 100% { transform: rotate(-12deg); }
						50% { transform: rotate(20deg); }
					}
				</style>
			</head>
			<body>
				<main>
					<div class="logo-wrap">
						<img class="logo" src="/img/electrodentallarge.png" alt="ElectrodentalNea">
					</div>
					<div class="scene" aria-hidden="true">
						<div class="monitor"><div class="gear"></div></div>
						<div class="worker">
							<div class="helmet"></div>
							<div class="head"></div>
							<div class="body"></div>
							<div class="arm"><div class="tool"></div></div>
						</div>
					</div>
					<div class="badge">Mantenimiento</div>
					<h1>Estamos en mantenimiento</h1>
					<p>Visitanos nuevamente dentro de un rato. Estamos ajustando la tienda para que todo funcione mejor.</p>
				</main>
			</body>
			</html>
			""";
}
