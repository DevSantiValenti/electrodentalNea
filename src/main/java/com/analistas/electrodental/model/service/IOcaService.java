package com.analistas.electrodental.model.service;

import java.util.List;

import com.analistas.electrodental.model.domain.Envio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.dto.OcaCotizacionResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaCreacionEnvioResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaSucursalDTO;

public interface IOcaService {

	OcaCotizacionResponseDTO cotizar(Pedido pedido);

	OcaCotizacionResponseDTO cotizarDomicilio(Pedido pedido);

	OcaCotizacionResponseDTO cotizarSucursal(Pedido pedido);

	List<OcaSucursalDTO> obtenerSucursales(String codigoPostal);

	Envio guardarCotizacion(Pedido pedido, OcaCotizacionResponseDTO cotizacion);

	Envio guardarCotizacion(Pedido pedido, OcaCotizacionResponseDTO cotizacion, String tipoEntregaOca, OcaSucursalDTO sucursal);

	OcaCreacionEnvioResponseDTO crearEnvio(Pedido pedido);

	byte[] obtenerEtiquetaPdf(Pedido pedido);

	String obtenerEtiquetaHtml(Pedido pedido);
}
