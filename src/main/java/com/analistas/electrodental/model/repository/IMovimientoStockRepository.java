package com.analistas.electrodental.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.MovimientoStock;

public interface IMovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

	List<MovimientoStock> findByProductoIdOrderByFechaDesc(Long productoId);
}
