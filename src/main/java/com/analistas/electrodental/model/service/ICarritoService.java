package com.analistas.electrodental.model.service;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;

public interface ICarritoService {

	CarritoDTO nuevoCarrito();

	CarritoDTO agregarProducto(CarritoDTO carrito, Producto producto, Integer cantidad);

	CarritoDTO actualizarCantidad(CarritoDTO carrito, Long productoId, Integer cantidad);

	CarritoDTO actualizarCantidad(CarritoDTO carrito, Producto producto, Integer cantidad);

	CarritoDTO quitarProducto(CarritoDTO carrito, Long productoId);
}
