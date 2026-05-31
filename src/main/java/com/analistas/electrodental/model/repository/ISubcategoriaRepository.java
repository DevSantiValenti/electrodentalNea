package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.Subcategoria;

public interface ISubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

	List<Subcategoria> findByActivoTrueOrderByNombreAsc();

	List<Subcategoria> findByCategoriaIdAndActivoTrueOrderByNombreAsc(Long categoriaId);

	Optional<Subcategoria> findBySlugAndActivoTrue(String slug);
}
