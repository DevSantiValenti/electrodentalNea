package com.analistas.electrodental.model.service;

import com.analistas.electrodental.model.domain.Producto;

public interface IStockService {

	void reservarStockWeb(Producto producto, Integer cantidad, String referencia);

	void liberarReservaWeb(Producto producto, Integer cantidad, String referencia);

	void registrarVentaWeb(Producto producto, Integer cantidad, String referencia);

	void registrarVentaFisica(Producto producto, Integer cantidad, String referencia);

	void ajustarStock(Producto producto, Integer nuevoStockWeb, Integer nuevoStockFisico, String referencia);
}
