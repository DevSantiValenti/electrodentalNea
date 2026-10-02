package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.Curso;

public interface ICursoRepository extends JpaRepository<Curso, Long> {

	List<Curso> findAllByOrderByOrdenAscTituloAsc();

	List<Curso> findByActivoTrueOrderByOrdenAscTituloAsc();

	Optional<Curso> findBySlugAndActivoTrue(String slug);

	Optional<Curso> findBySlug(String slug);

	@Query("""
			select distinct c
			from Curso c
			join c.areas a
			where c.activo = true
				and a.activo = true
				and a.id = :areaId
			order by c.orden asc, c.titulo asc
			""")
	List<Curso> findActivosByAreaId(@Param("areaId") Long areaId);

	@Query("select count(c) from Curso c where c.activo = true")
	long contarActivos();
}
