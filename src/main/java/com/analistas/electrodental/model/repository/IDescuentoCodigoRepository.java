package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.DescuentoCodigo;

import jakarta.persistence.LockModeType;

public interface IDescuentoCodigoRepository extends JpaRepository<DescuentoCodigo, Long> {

	@EntityGraph(attributePaths = { "productos", "categorias", "subcategorias" })
	List<DescuentoCodigo> findAllByOrderByFechaCreacionDesc();

	@EntityGraph(attributePaths = { "productos", "categorias", "subcategorias" })
	Optional<DescuentoCodigo> findByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	@EntityGraph(attributePaths = { "productos", "categorias", "subcategorias" })
	@Query("select d from DescuentoCodigo d where d.id = :id")
	Optional<DescuentoCodigo> findDetalleById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DescuentoCodigo d where upper(d.codigo) = upper(:codigo)")
	Optional<DescuentoCodigo> findByCodigoForUpdate(@Param("codigo") String codigo);
}
