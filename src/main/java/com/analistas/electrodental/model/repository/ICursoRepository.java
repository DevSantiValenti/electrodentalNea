package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.analistas.electrodental.model.domain.Curso;

public interface ICursoRepository extends JpaRepository<Curso, Long> {

	List<Curso> findAllByOrderByOrdenAscTituloAsc();

	List<Curso> findByActivoTrueOrderByOrdenAscTituloAsc();

	Optional<Curso> findBySlugAndActivoTrue(String slug);

	Optional<Curso> findBySlug(String slug);

	@Query("select count(c) from Curso c where c.activo = true")
	long contarActivos();
}
