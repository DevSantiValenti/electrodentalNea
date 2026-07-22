package com.analistas.electrodental.web.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class ErrorPageModelFactory {

	private static final Logger log = LoggerFactory.getLogger(ErrorPageModelFactory.class);
	private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final String WHATSAPP_SOPORTE = "543624701036";

	public ModelAndView crear(HttpServletRequest request, Throwable exception, HttpStatus statusFallback, String mensajeFallback) {
		HttpStatus status = resolverStatus(request, statusFallback);
		String fechaHora = ZonedDateTime.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires"))
				.format(FECHA_FORMATTER);
		String codigo = "ERR-" + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ "-" + status.value()
				+ "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		String metodo = request.getMethod();
		String ruta = resolverTexto(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), request.getRequestURI());
		String mensaje = resolverMensaje(request, exception, mensajeFallback, status);
		String tipoError = resolverTipoError(request, exception, status);

		registrarError(codigo, status, metodo, ruta, mensaje, exception, request);

		ModelAndView modelAndView = new ModelAndView("error");
		modelAndView.setStatus(status);
		modelAndView.addObject("codigoError", codigo);
		modelAndView.addObject("fechaHoraError", fechaHora);
		modelAndView.addObject("estadoError", status.value());
		modelAndView.addObject("tituloError", titulo(status));
		modelAndView.addObject("descripcionError", descripcion(status));
		modelAndView.addObject("nombreError", tipoError);
		modelAndView.addObject("mensajeError", mensaje);
		modelAndView.addObject("metodoError", metodo);
		modelAndView.addObject("rutaError", ruta);
		modelAndView.addObject("refererError", request.getHeader("Referer"));
		modelAndView.addObject("whatsappErrorLink", whatsappLink(codigo, fechaHora, status, metodo, ruta, tipoError));
		return modelAndView;
	}

	private HttpStatus resolverStatus(HttpServletRequest request, HttpStatus fallback) {
		Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		if (statusCode instanceof Integer code) {
			HttpStatus status = HttpStatus.resolve(code);
			if (status != null) {
				return status;
			}
		}
		return fallback == null ? HttpStatus.INTERNAL_SERVER_ERROR : fallback;
	}

	private String resolverMensaje(HttpServletRequest request, Throwable exception, String fallback, HttpStatus status) {
		if (exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()) {
			return exception.getMessage();
		}
		String requestMessage = resolverTexto(request.getAttribute(RequestDispatcher.ERROR_MESSAGE), null);
		if (requestMessage != null && !requestMessage.isBlank()) {
			return requestMessage;
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return status.getReasonPhrase();
	}

	private String resolverTipoError(HttpServletRequest request, Throwable exception, HttpStatus status) {
		if (exception != null) {
			return exception.getClass().getSimpleName();
		}
		Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
		if (exceptionType instanceof Class<?> type) {
			return type.getSimpleName();
		}
		return status.getReasonPhrase();
	}

	private String resolverTexto(Object valor, String fallback) {
		return valor == null ? fallback : String.valueOf(valor);
	}

	private String titulo(HttpStatus status) {
		return switch (status) {
			case NOT_FOUND -> "No encontramos esa pagina";
			case METHOD_NOT_ALLOWED -> "No pudimos procesar esa accion";
			case FORBIDDEN -> "No tenes permiso para ver esto";
			case UNAUTHORIZED -> "Necesitas iniciar sesion";
			default -> status.is5xxServerError() ? "Algo salio mal" : "No pudimos completar la solicitud";
		};
	}

	private String descripcion(HttpStatus status) {
		return switch (status) {
			case NOT_FOUND -> "La ruta que intentaste abrir no existe o fue movida.";
			case METHOD_NOT_ALLOWED -> "La pagina existe, pero se intento acceder con un metodo HTTP no permitido.";
			case FORBIDDEN -> "Tu usuario no tiene permisos para acceder a esta seccion.";
			case UNAUTHORIZED -> "Inicia sesion y volve a intentarlo.";
			default -> "Toma una captura de pantalla y enviamela contando que ocurrio para poder solucionarlo rapidamente.";
		};
	}

	private String whatsappLink(String codigo, String fechaHora, HttpStatus status, String metodo, String ruta, String tipoError) {
		String texto = "Hola, me aparecio un error en Electrodental NEA.\n"
				+ "Codigo: " + codigo + "\n"
				+ "Fecha y hora: " + fechaHora + "\n"
				+ "Estado: " + status.value() + "\n"
				+ "Error: " + tipoError + "\n"
				+ "Metodo: " + metodo + "\n"
				+ "Ruta: " + ruta + "\n"
				+ "Adjunto captura y te cuento que ocurrio: ";
		return "https://wa.me/" + WHATSAPP_SOPORTE + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8);
	}

	private void registrarError(
			String codigo,
			HttpStatus status,
			String metodo,
			String ruta,
			String mensaje,
			Throwable exception,
			HttpServletRequest request) {
		if (status.is5xxServerError()) {
			log.error(
					"Vista de error {} - estado={} metodo={} ruta={} mensaje={} referer={} userAgent={}",
					codigo,
					status.value(),
					metodo,
					ruta,
					mensaje,
					request.getHeader("Referer"),
					request.getHeader("User-Agent"),
					exception);
			return;
		}
		log.warn(
				"Vista de error {} - estado={} metodo={} ruta={} mensaje={} referer={} userAgent={}",
				codigo,
				status.value(),
				metodo,
				ruta,
				mensaje,
				request.getHeader("Referer"),
				request.getHeader("User-Agent"));
	}
}
