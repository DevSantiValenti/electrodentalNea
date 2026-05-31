package com.analistas.electrodental.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.VentaPresencial;

public interface IVentaPresencialRepository extends JpaRepository<VentaPresencial, Long> {

	List<VentaPresencial> findTop10ByOrderByFechaDesc();

	long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
