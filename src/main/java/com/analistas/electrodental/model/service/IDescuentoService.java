package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import com.analistas.electrodental.model.domain.DescuentoCodigo;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.dto.ResultadoDescuentoDTO;

public interface IDescuentoService {

	List<DescuentoCodigo> listarTodos();

	Optional<DescuentoCodigo> buscarPorId(Long id);

	DescuentoCodigo nuevoDescuento();

	DescuentoCodigo guardar(
			Long id,
			DescuentoCodigo datos,
			List<Long> productoIds,
			List<Long> categoriaIds,
			List<Long> subcategoriaIds);

	void eliminar(Long id);

	ResultadoDescuentoDTO aplicar(CarritoDTO carrito, String codigo);

	CarritoDTO recalcular(CarritoDTO carrito);

	void registrarUso(String codigo);
}
