package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.repository.ICategoriaRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.ISubcategoriaRepository;

@Service
@Transactional(readOnly = true)
public class CategoriaServiceImpl implements ICategoriaService {

	private final ICategoriaRepository categoriaRepository;
	private final ISubcategoriaRepository subcategoriaRepository;
	private final IProductoRepository productoRepository;

	public CategoriaServiceImpl(
			ICategoriaRepository categoriaRepository,
			ISubcategoriaRepository subcategoriaRepository,
			IProductoRepository productoRepository) {
		this.categoriaRepository = categoriaRepository;
		this.subcategoriaRepository = subcategoriaRepository;
		this.productoRepository = productoRepository;
	}

	@Override
	public List<Categoria> listarActivas() {
		return categoriaRepository.findByActivoTrueOrderByNombreAsc();
	}

	@Override
	public List<Categoria> listarTodas() {
		return categoriaRepository.findAll();
	}

	@Override
	public List<Subcategoria> listarSubcategoriasActivasPorCategoria(Long categoriaId) {
		return subcategoriaRepository.findByCategoriaIdAndActivoTrueOrderByNombreAsc(categoriaId);
	}

	@Override
	public Optional<Categoria> buscarCategoriaPorId(Long id) {
		return categoriaRepository.findById(id);
	}

	@Override
	public Optional<Categoria> buscarCategoriaPorSlug(String slug) {
		return categoriaRepository.findBySlugAndActivoTrue(slug);
	}

	@Override
	public Optional<Subcategoria> buscarSubcategoriaPorId(Long id) {
		return subcategoriaRepository.findById(id);
	}

	@Override
	public Optional<Subcategoria> buscarSubcategoriaPorSlug(String slug) {
		return subcategoriaRepository.findBySlugAndActivoTrue(slug);
	}

	@Override
	@Transactional
	public Categoria guardarCategoria(Categoria categoria) {
		if (categoria.getId() != null) {
			Categoria actual = categoriaRepository.findById(categoria.getId())
					.orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + categoria.getId()));
			actual.setNombre(categoria.getNombre());
			actual.setSlug(categoria.getSlug());
			actual.setDescripcion(categoria.getDescripcion());
			actual.setActivo(categoria.getActivo());
			return categoriaRepository.save(actual);
		}
		return categoriaRepository.save(categoria);
	}

	@Override
	@Transactional
	public Subcategoria guardarSubcategoria(Long categoriaId, Subcategoria subcategoria) {
		Categoria categoria = categoriaRepository.findById(categoriaId)
				.orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + categoriaId));
		if (subcategoria.getId() != null) {
			Subcategoria actual = subcategoriaRepository.findById(subcategoria.getId())
					.orElseThrow(() -> new IllegalArgumentException("Subcategoria no encontrada: " + subcategoria.getId()));
			actual.setNombre(subcategoria.getNombre());
			actual.setSlug(subcategoria.getSlug());
			actual.setDescripcion(subcategoria.getDescripcion());
			actual.setActivo(subcategoria.getActivo());
			actual.setCategoria(categoria);
			return subcategoriaRepository.save(actual);
		}
		subcategoria.setCategoria(categoria);
		return subcategoriaRepository.save(subcategoria);
	}

	@Override
	@Transactional
	public void eliminarCategoria(Long id) {
		Categoria categoria = categoriaRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + id));
		productoRepository.desvincularSubcategoriasDeCategoria(id);
		productoRepository.desvincularCategoria(id);
		categoriaRepository.delete(categoria);
	}

	@Override
	@Transactional
	public void eliminarSubcategoria(Long id) {
		Subcategoria subcategoria = subcategoriaRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Subcategoria no encontrada: " + id));
		productoRepository.desvincularSubcategoria(id);
		subcategoriaRepository.delete(subcategoria);
	}
}
