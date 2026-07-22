package com.analistas.electrodental.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class HttpErrorLoggingAdvice {

	private static final Logger log = LoggerFactory.getLogger(HttpErrorLoggingAdvice.class);
	private final ErrorPageModelFactory errorPageModelFactory;

	public HttpErrorLoggingAdvice(ErrorPageModelFactory errorPageModelFactory) {
		this.errorPageModelFactory = errorPageModelFactory;
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ModelAndView methodNotSupported(
			HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		log.warn(
				"Metodo HTTP no soportado: {} {}. Metodos permitidos: {}. Referer: {}. User-Agent: {}",
				request.getMethod(),
				request.getRequestURI(),
				ex.getSupportedHttpMethods(),
				request.getHeader("Referer"),
				request.getHeader("User-Agent"));
		return errorPageModelFactory.crear(request, ex, HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no soportado.");
	}
}
