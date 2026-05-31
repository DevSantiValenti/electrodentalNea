package com.analistas.electrodental.model.service;

import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.dto.AndreaniCotizacionResponseDTO;

public interface IAndreaniService {

	AndreaniCotizacionResponseDTO cotizar(Pedido pedido);
}
