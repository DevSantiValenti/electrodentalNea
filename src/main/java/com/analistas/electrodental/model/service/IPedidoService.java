package com.analistas.electrodental.model.service;

import java.math.BigDecimal;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;

public interface IPedidoService {

	Pedido crearPedidoWeb(Cliente cliente, DireccionEnvio direccionEnvio, CarritoDTO carrito);

	Pedido crearPedidoWeb(Cliente cliente, DireccionEnvio direccionEnvio, CarritoDTO carrito, String metodoEntrega, BigDecimal costoEnvio);

	Pedido marcarPagado(String externalReference, String paymentId);

	Pedido actualizarPagoMercadoPago(String externalReference, String paymentId, String status);

	Pedido cancelarPedido(Long pedidoId, String motivo);
}
