package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.CursoArea;

public interface ICursoAreaRepository extends JpaRepository<CursoArea, Long> {

	List<CursoArea> findAllByOrderByOrdenAscNombreAsc();

	List<CursoArea> findByActivoTrueOrderByOrdenAscNombreAsc();

	Optional<CursoArea> findBySlug(String slug);

	Optional<CursoArea> findBySlugAndActivoTrue(String slug);
}
