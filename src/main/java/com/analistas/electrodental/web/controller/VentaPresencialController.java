package com.analistas.electrodental.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.MetodoPagoVenta;
import com.analistas.electrodental.model.domain.dto.VentaPresencialRequestDTO;
import com.analistas.electrodental.model.domain.dto.VentaPresencialItemRequestDTO;
import com.analistas.electrodental.model.service.IProductoService;
import com.analistas.electrodental.model.service.IVentaPresencialService;

@Controller
public class VentaPresencialController {

	private final IVentaPresencialService ventaPresencialService;
	private final IProductoService productoService;

	public VentaPresencialController(IVentaPresencialService ventaPresencialService, IProductoService productoService) {
		this.ventaPresencialService = ventaPresencialService;
		this.productoService = productoService;
	}

	@GetMapping("/admin/ventas/nueva")
	public String nueva(Model model) {
		model.addAttribute("productos", productoService.listarActivos());
		model.addAttribute("metodosPago", MetodoPagoVenta.values());
		return "admin/ventas-nueva";
	}

	@PostMapping("/admin/ventas/nueva")
	public String registrar(
			@RequestParam Long productoId,
			@RequestParam(defaultValue = "1") Integer cantidad,
			@RequestParam(defaultValue = "EFECTIVO") MetodoPagoVenta metodoPago,
			@RequestParam(required = false) String usuarioAdmin,
			@RequestParam(required = false) String observaciones,
			RedirectAttributes redirectAttributes) {
		VentaPresencialRequestDTO request = new VentaPresencialRequestDTO(
				List.of(new VentaPresencialItemRequestDTO(productoId, cantidad)),
				metodoPago,
				usuarioAdmin,
				observaciones);
		ventaPresencialService.registrarVenta(request);
		redirectAttributes.addFlashAttribute("mensaje", "Venta presencial registrada");
		return "redirect:/admin/ventas";
	}
}
