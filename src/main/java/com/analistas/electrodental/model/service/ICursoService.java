package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import com.analistas.electrodental.model.domain.Curso;

public interface ICursoService {

	List<Curso> listarTodos();

	List<Curso> listarActivos();

	Optional<Curso> buscarPorId(Long id);

	Optional<Curso> buscarActivoPorSlug(String slug);

	Curso guardar(Curso curso);

	void desactivar(Long id);

	long contarActivos();

	long contarClasesActivas();
}
