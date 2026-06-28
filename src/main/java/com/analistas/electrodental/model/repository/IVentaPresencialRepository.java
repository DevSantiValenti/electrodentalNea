package com.analistas.electrodental.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.VentaPresencial;

public interface IVentaPresencialRepository extends JpaRepository<VentaPresencial, Long> {

	@EntityGraph(attributePaths = { "cliente", "items", "items.producto" })
	List<VentaPresencial> findTop10ByOrderByFechaDesc();

	@EntityGraph(attributePaths = { "cliente", "items", "items.producto" })
	@Query("select v from VentaPresencial v where v.id = :id")
	Optional<VentaPresencial> findDetalleById(@Param("id") Long id);

	@EntityGraph(attributePaths = { "cliente", "items" })
	@Query(
			value = """
					select v
					from VentaPresencial v
					left join v.cliente c
					where (:termino is null
						or str(v.id) like :termino
						or str(v.total) like :termino
						or (:monto is not null and v.total = :monto)
						or lower(str(v.metodoPago)) like :termino
						or lower(coalesce(c.nombre, '')) like :termino
						or lower(coalesce(c.apellidoRazonSocial, '')) like :termino
						or lower(coalesce(c.dniCuit, '')) like :termino
						or lower(coalesce(c.email, '')) like :termino)
					and (:fechaDesde is null or v.fecha >= :fechaDesde)
					and (:fechaHasta is null or v.fecha <= :fechaHasta)
					order by v.fecha desc, v.id desc
					""",
			countQuery = """
					select count(v)
					from VentaPresencial v
					left join v.cliente c
					where (:termino is null
						or str(v.id) like :termino
						or str(v.total) like :termino
						or (:monto is not null and v.total = :monto)
						or lower(str(v.metodoPago)) like :termino
						or lower(coalesce(c.nombre, '')) like :termino
						or lower(coalesce(c.apellidoRazonSocial, '')) like :termino
						or lower(coalesce(c.dniCuit, '')) like :termino
						or lower(coalesce(c.email, '')) like :termino)
					and (:fechaDesde is null or v.fecha >= :fechaDesde)
					and (:fechaHasta is null or v.fecha <= :fechaHasta)
					""")
	Page<VentaPresencial> buscarDetalle(
			@Param("termino") String termino,
			@Param("monto") BigDecimal monto,
			@Param("fechaDesde") LocalDateTime fechaDesde,
			@Param("fechaHasta") LocalDateTime fechaHasta,
			Pageable pageable);

	long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
