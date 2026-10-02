package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import com.analistas.electrodental.model.domain.CursoArea;

public interface ICursoAreaService {

	List<CursoArea> listarTodos();

	List<CursoArea> listarActivas();

	Optional<CursoArea> buscarPorId(Long id);

	Optional<CursoArea> buscarActivaPorSlug(String slug);

	List<CursoArea> buscarPorIds(List<Long> ids);

	CursoArea guardar(CursoArea area);

	void desactivar(Long id);
}
