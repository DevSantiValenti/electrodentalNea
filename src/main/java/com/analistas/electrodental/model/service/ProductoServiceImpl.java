package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.repository.IProductoRepository;

@Service
@Transactional(readOnly = true)
public class ProductoServiceImpl implements IProductoService {

	private final IProductoRepository productoRepository;

	public ProductoServiceImpl(IProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	@Override
	public List<Producto> listarActivos() {
		return productoRepository.findByActivoTrueOrderByNombreAsc();
	}

	@Override
	public List<Producto> listarPorCategoria(String categoriaSlug) {
		if (categoriaSlug == null || categoriaSlug.isBlank()) {
			return listarActivos();
		}
		return productoRepository.findByActivoTrueAndCategoriaSlugOrderByNombreAsc(categoriaSlug);
	}

	@Override
	public List<Producto> listarPorSubcategoria(String subcategoriaSlug) {
		if (subcategoriaSlug == null || subcategoriaSlug.isBlank()) {
			return listarActivos();
		}
		return productoRepository.findByActivoTrueAndSubcategoriaSlugOrderByNombreAsc(subcategoriaSlug);
	}

	@Override
	public List<Producto> listarDestacados() {
		return productoRepository.findByActivoTrueAndDestacadoTrueOrderByNombreAsc();
	}

	@Override
	public List<Producto> listarOfertas() {
		return productoRepository.findByActivoTrueAndOfertaTrueOrderByNombreAsc();
	}

	@Override
	public List<Producto> listarBajoStock() {
		return productoRepository.findProductosConBajoStock();
	}

	@Override
	public List<Producto> listarRelacionados(Producto producto) {
		if (producto == null || producto.getCategoria() == null || producto.getId() == null) {
			return listarDestacados().stream().limit(4).toList();
		}
		return productoRepository.findTop4ByActivoTrueAndCategoriaIdAndIdNotOrderByNombreAsc(
				producto.getCategoria().getId(),
				producto.getId());
	}

	@Override
	public List<Producto> buscarSugerencias(String termino, int limite) {
		if (termino == null || termino.isBlank()) {
			return List.of();
		}
		int limiteSeguro = Math.max(1, Math.min(limite, 12));
		return productoRepository.buscarSugerencias(termino.trim(), PageRequest.of(0, limiteSeguro));
	}

	@Override
	public Optional<Producto> buscarPorSlug(String slug) {
		return productoRepository.findBySlugAndActivoTrue(slug);
	}

	@Override
	public Optional<Producto> buscarPorId(Long id) {
		return productoRepository.findById(id);
	}

	@Override
	@Transactional
	public Producto guardar(Producto producto) {
		return productoRepository.save(producto);
	}
}
