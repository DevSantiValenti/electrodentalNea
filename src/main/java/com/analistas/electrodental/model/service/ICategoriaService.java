package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.Subcategoria;

public interface ICategoriaService {

	List<Categoria> listarActivas();

	List<Categoria> listarTodas();

	List<Subcategoria> listarSubcategoriasActivasPorCategoria(Long categoriaId);

	Optional<Categoria> buscarCategoriaPorId(Long id);

	Optional<Categoria> buscarCategoriaPorSlug(String slug);

	Optional<Subcategoria> buscarSubcategoriaPorId(Long id);

	Optional<Subcategoria> buscarSubcategoriaPorSlug(String slug);

	Categoria guardarCategoria(Categoria categoria);

	Subcategoria guardarSubcategoria(Long categoriaId, Subcategoria subcategoria);
}
