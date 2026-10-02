package com.analistas.electrodental.model.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.domain.Curso;
import com.analistas.electrodental.model.domain.CursoArea;
import com.analistas.electrodental.model.repository.ICursoAreaRepository;

@Service
@Transactional(readOnly = true)
public class CursoAreaServiceImpl implements ICursoAreaService {

	private final ICursoAreaRepository areaRepository;

	public CursoAreaServiceImpl(ICursoAreaRepository areaRepository) {
		this.areaRepository = areaRepository;
	}

	@Override
	public List<CursoArea> listarTodos() {
		return areaRepository.findAllByOrderByOrdenAscNombreAsc();
	}

	@Override
	public List<CursoArea> listarActivas() {
		return areaRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
	}

	@Override
	public Optional<CursoArea> buscarPorId(Long id) {
		return areaRepository.findById(id);
	}

	@Override
	public Optional<CursoArea> buscarActivaPorSlug(String slug) {
		String slugNormalizado = Curso.normalizarSlug(slug);
		if (!StringUtils.hasText(slugNormalizado)) {
			return Optional.empty();
		}
		Optional<CursoArea> area = areaRepository.findBySlugAndActivoTrue(slugNormalizado);
		if (area.isPresent()) {
			return area;
		}
		return areaRepository.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
				.filter(candidata -> slugNormalizado.equals(candidata.getSlug()))
				.findFirst();
	}

	@Override
	public List<CursoArea> buscarPorIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return areaRepository.findAllById(ids).stream()
				.sorted(Comparator.comparing(CursoArea::ordenSeguro).thenComparing(CursoArea::nombreSeguro))
				.toList();
	}

	@Override
	@Transactional
	public CursoArea guardar(CursoArea area) {
		CursoArea destino = area.getId() == null
				? new CursoArea()
				: areaRepository.findById(area.getId())
						.orElseThrow(() -> new IllegalArgumentException("Area no encontrada: " + area.getId()));
		destino.setNombre(normalizar(area.getNombre()));
		destino.setSlug(generarSlug(StringUtils.hasText(area.getSlug()) ? area.getSlug() : area.getNombre()));
		destino.setDescripcion(normalizar(area.getDescripcion()));
		destino.setOrden(area.getOrden() == null ? 0 : area.getOrden());
		destino.setActivo(area.getActivo() == null || area.getActivo());
		return areaRepository.save(destino);
	}

	@Override
	@Transactional
	public void desactivar(Long id) {
		areaRepository.findById(id).ifPresent(area -> {
			area.setActivo(false);
			areaRepository.save(area);
		});
	}

	private String generarSlug(String valor) {
		String slug = Curso.normalizarSlug(valor);
		if (!StringUtils.hasText(slug)) {
			return "area-" + System.currentTimeMillis();
		}
		return slug;
	}

	private String normalizar(String valor) {
		return valor == null ? "" : valor.trim();
	}
}
