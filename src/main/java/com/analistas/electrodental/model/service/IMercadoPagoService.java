package com.analistas.electrodental.model.service;

import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.dto.MercadoPagoPreferenceResponseDTO;

public interface IMercadoPagoService {

	MercadoPagoPreferenceResponseDTO crearPreferencia(Pedido pedido);
}
