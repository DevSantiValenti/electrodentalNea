package com.analistas.electrodental.model.service;

import com.analistas.electrodental.model.domain.VentaPresencial;
import com.analistas.electrodental.model.domain.dto.VentaPresencialRequestDTO;

public interface IVentaPresencialService {

	VentaPresencial registrarVenta(VentaPresencialRequestDTO request);

	VentaPresencial obtenerVenta(Long id);

	VentaPresencial actualizarVenta(Long id, VentaPresencialRequestDTO request);

	void eliminarVenta(Long id);
}
