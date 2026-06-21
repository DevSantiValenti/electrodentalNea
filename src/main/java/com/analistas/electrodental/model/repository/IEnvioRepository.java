package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.Envio;

public interface IEnvioRepository extends JpaRepository<Envio, Long> {

	Optional<Envio> findByPedidoId(Long pedidoId);

	@EntityGraph(attributePaths = { "pedido" })
	Optional<Envio> findDetalleById(Long id);

	@Query("""
			select e
			from Envio e
			join fetch e.pedido p
			join fetch p.cliente
			left join fetch p.direccionEnvio
			order by coalesce(e.fechaCreacionEnvio, e.fechaCotizacion) desc, e.id desc
			""")
	List<Envio> findAllDetalleOrderByFechaDesc();

	@Query(
			value = """
					select e
					from Envio e
					join fetch e.pedido p
					join fetch p.cliente c
					left join fetch p.direccionEnvio
					where (:termino is null
						or lower(coalesce(p.codigoCompra, '')) like :termino
						or str(p.id) like :termino
						or lower(coalesce(c.nombre, '')) like :termino
						or lower(coalesce(c.apellidoRazonSocial, '')) like :termino
						or lower(coalesce(c.email, '')) like :termino
						or lower(coalesce(e.numeroOrdenRetiro, '')) like :termino
						or lower(coalesce(e.numeroEnvio, '')) like :termino
						or lower(coalesce(e.tracking, '')) like :termino)
					and (:fechaDesde is null or coalesce(e.fechaCreacionEnvio, e.fechaCotizacion) >= :fechaDesde)
					and (:fechaHasta is null or coalesce(e.fechaCreacionEnvio, e.fechaCotizacion) <= :fechaHasta)
					order by coalesce(e.fechaCreacionEnvio, e.fechaCotizacion) desc, e.id desc
					""",
			countQuery = """
					select count(e)
					from Envio e
					join e.pedido p
					join p.cliente c
					where (:termino is null
						or lower(coalesce(p.codigoCompra, '')) like :termino
						or str(p.id) like :termino
						or lower(coalesce(c.nombre, '')) like :termino
						or lower(coalesce(c.apellidoRazonSocial, '')) like :termino
						or lower(coalesce(c.email, '')) like :termino
						or lower(coalesce(e.numeroOrdenRetiro, '')) like :termino
						or lower(coalesce(e.numeroEnvio, '')) like :termino
						or lower(coalesce(e.tracking, '')) like :termino)
					and (:fechaDesde is null or coalesce(e.fechaCreacionEnvio, e.fechaCotizacion) >= :fechaDesde)
					and (:fechaHasta is null or coalesce(e.fechaCreacionEnvio, e.fechaCotizacion) <= :fechaHasta)
					""")
	Page<Envio> buscarDetalle(
			@Param("termino") String termino,
			@Param("fechaDesde") LocalDateTime fechaDesde,
			@Param("fechaHasta") LocalDateTime fechaHasta,
			Pageable pageable);
}
