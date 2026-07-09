package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import com.analistas.electrodental.model.domain.Producto;

public interface IProductoService {

	List<Producto> listarTodos();

	List<Producto> filtrarAdmin(Long categoriaId, Long subcategoriaId, boolean soloBajoStock, boolean soloOfertas);

	List<Producto> listarActivos();

	List<Producto> listarPorCategoria(String categoriaSlug);

	List<Producto> listarPorSubcategoria(String subcategoriaSlug);

	List<Producto> listarDestacados();

	List<Producto> listarOfertas();

	List<Producto> listarBajoStock();

	long contarBajoStockAdmin();

	long contarOfertasAdmin();

	List<Producto> listarRelacionados(Producto producto);

	List<Producto> buscarSugerencias(String termino, int limite);

	Optional<Producto> buscarPorSlug(String slug);

	Optional<Producto> buscarPorId(Long id);

	Producto guardar(Producto producto);

	void eliminar(Long id);
}
