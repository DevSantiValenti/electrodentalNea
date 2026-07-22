package com.analistas.electrodental.web.controller;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

	private final ErrorPageModelFactory errorPageModelFactory;

	public CustomErrorController(ErrorPageModelFactory errorPageModelFactory) {
		this.errorPageModelFactory = errorPageModelFactory;
	}

	@RequestMapping("/error")
	public ModelAndView error(HttpServletRequest request) {
		return errorPageModelFactory.crear(request, null, null, null);
	}
}
