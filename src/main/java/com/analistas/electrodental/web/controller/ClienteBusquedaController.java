package com.analistas.electrodental.web.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.dto.ClienteBusquedaDTO;
import com.analistas.electrodental.model.repository.IClienteRepository;

@RestController
public class ClienteBusquedaController {

	private final IClienteRepository clienteRepository;

	public ClienteBusquedaController(IClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

	@GetMapping("/api/clientes/buscar")
	public List<ClienteBusquedaDTO> buscar(@RequestParam(name = "q", required = false) String termino) {
		if (!StringUtils.hasText(termino) || termino.trim().length() < 2) {
			return List.of();
		}
		String terminoNormalizado = termino.replaceAll("[^0-9]", "").trim();
		String busqueda = terminoNormalizado.length() >= 2 ? terminoNormalizado : termino.trim();
		return clienteRepository.buscarSugerencias(busqueda, PageRequest.of(0, 8))
				.stream()
				.map(this::toDto)
				.toList();
	}

	private ClienteBusquedaDTO toDto(Cliente cliente) {
		String nombreCompleto = (valor(cliente.getNombre()) + " " + valor(cliente.getApellidoRazonSocial())).trim();
		return new ClienteBusquedaDTO(
				cliente.getId(),
				cliente.getDniCuit(),
				cliente.getNombre(),
				cliente.getApellidoRazonSocial(),
				cliente.getEmail(),
				cliente.getTelefono(),
				nombreCompleto.isBlank() ? cliente.getDniCuit() : nombreCompleto);
	}

	private String valor(String valor) {
		return valor == null ? "" : valor;
	}
}
