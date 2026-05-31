package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.repository.ICategoriaRepository;
import com.analistas.electrodental.model.repository.ISubcategoriaRepository;

@Service
@Transactional(readOnly = true)
public class CategoriaServiceImpl implements ICategoriaService {

	private final ICategoriaRepository categoriaRepository;
	private final ISubcategoriaRepository subcategoriaRepository;

	public CategoriaServiceImpl(ICategoriaRepository categoriaRepository, ISubcategoriaRepository subcategoriaRepository) {
		this.categoriaRepository = categoriaRepository;
		this.subcategoriaRepository = subcategoriaRepository;
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
		return categoriaRepository.save(categoria);
	}

	@Override
	@Transactional
	public Subcategoria guardarSubcategoria(Long categoriaId, Subcategoria subcategoria) {
		Categoria categoria = categoriaRepository.findById(categoriaId)
				.orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + categoriaId));
		subcategoria.setCategoria(categoria);
		return subcategoriaRepository.save(subcategoria);
	}
}
