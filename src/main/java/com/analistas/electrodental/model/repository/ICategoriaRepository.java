package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.Categoria;

public interface ICategoriaRepository extends JpaRepository<Categoria, Long> {

	List<Categoria> findByActivoTrueOrderByNombreAsc();

	Optional<Categoria> findBySlugAndActivoTrue(String slug);
}
