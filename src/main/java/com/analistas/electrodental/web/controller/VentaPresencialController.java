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
import com.analistas.electrodental.model.service.IVentaPresencialService;

@Controller
public class VentaPresencialController {

	private final IVentaPresencialService ventaPresencialService;

	public VentaPresencialController(IVentaPresencialService ventaPresencialService) {
		this.ventaPresencialService = ventaPresencialService;
	}

	@GetMapping("/admin/ventas/nueva")
	public String nueva(Model model) {
		model.addAttribute("metodosPago", MetodoPagoVenta.values());
		return "admin/ventas-nueva";
	}

	@PostMapping("/admin/ventas/nueva")
	public String registrar(
			@RequestParam(name = "productoIds") List<Long> productoIds,
			@RequestParam(name = "cantidades") List<Integer> cantidades,
			@RequestParam String clienteDniCuit,
			@RequestParam(required = false) String clienteNombre,
			@RequestParam(required = false) String clienteApellidoRazonSocial,
			@RequestParam(required = false) String clienteEmail,
			@RequestParam(required = false) String clienteTelefono,
			@RequestParam(defaultValue = "EFECTIVO") MetodoPagoVenta metodoPago,
			@RequestParam(required = false) String usuarioAdmin,
			@RequestParam(required = false) String observaciones,
			RedirectAttributes redirectAttributes) {
		if (productoIds.size() != cantidades.size()) {
			redirectAttributes.addFlashAttribute("mensaje", "Revisá las lineas de venta: falta producto o cantidad.");
			return "redirect:/admin/ventas/nueva";
		}
		VentaPresencialRequestDTO request = new VentaPresencialRequestDTO(
				crearItems(productoIds, cantidades),
				clienteDniCuit,
				clienteNombre,
				clienteApellidoRazonSocial,
				clienteEmail,
				clienteTelefono,
				metodoPago,
				usuarioAdmin,
				observaciones);
		try {
			ventaPresencialService.registrarVenta(request);
			redirectAttributes.addFlashAttribute("mensaje", "Venta presencial registrada");
		} catch (IllegalArgumentException | IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("mensaje", ex.getMessage());
			return "redirect:/admin/ventas/nueva";
		}
		return "redirect:/admin/ventas";
	}

	private List<VentaPresencialItemRequestDTO> crearItems(List<Long> productoIds, List<Integer> cantidades) {
		return java.util.stream.IntStream.range(0, productoIds.size())
				.mapToObj(indice -> new VentaPresencialItemRequestDTO(productoIds.get(indice), cantidades.get(indice)))
				.toList();
	}
}
