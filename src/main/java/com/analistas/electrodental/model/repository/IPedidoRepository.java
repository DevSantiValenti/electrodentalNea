package com.analistas.electrodental.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.Pedido;

public interface IPedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findTop10ByOrderByFechaCreacionDesc();

	@Query(
			value = """
					select p
					from Pedido p
					join fetch p.cliente c
					left join fetch p.pago
					left join fetch p.envio
					where (:termino is null
						or lower(coalesce(p.codigoCompra, '')) like :termino
						or str(p.id) like :termino
						or lower(coalesce(p.codigoDescuento, '')) like :termino
						or lower(coalesce(c.nombre, '')) like :termino
						or lower(coalesce(c.apellidoRazonSocial, '')) like :termino
						or lower(coalesce(c.email, '')) like :termino
						or lower(coalesce(c.dniCuit, '')) like :termino)
					and (:fechaDesde is null or p.fechaCreacion >= :fechaDesde)
					and (:fechaHasta is null or p.fechaCreacion <= :fechaHasta)
					order by p.fechaCreacion desc, p.id desc
					""",
			countQuery = """
					select count(p)
					from Pedido p
					join p.cliente c
					where (:termino is null
						or lower(coalesce(p.codigoCompra, '')) like :termino
						or str(p.id) like :termino
						or lower(coalesce(p.codigoDescuento, '')) like :termino
						or lower(coalesce(c.nombre, '')) like :termino
						or lower(coalesce(c.apellidoRazonSocial, '')) like :termino
						or lower(coalesce(c.email, '')) like :termino
						or lower(coalesce(c.dniCuit, '')) like :termino)
					and (:fechaDesde is null or p.fechaCreacion >= :fechaDesde)
					and (:fechaHasta is null or p.fechaCreacion <= :fechaHasta)
					""")
	Page<Pedido> buscarDetalle(
			@Param("termino") String termino,
			@Param("fechaDesde") LocalDateTime fechaDesde,
			@Param("fechaHasta") LocalDateTime fechaHasta,
			Pageable pageable);

	@EntityGraph(attributePaths = { "cliente", "direccionEnvio", "items", "items.producto", "pago", "envio" })
	Optional<Pedido> findDetalleById(Long id);

	long countByFechaCreacionBetween(LocalDateTime desde, LocalDateTime hasta);

	long countByEstadoPedido(EstadoPedido estadoPedido);

	@Query("""
			select distinct p from Pedido p
			join fetch p.items items
			join fetch items.producto
			join fetch p.pago pago
			where p.estadoPedido = :estadoPedido
			  and p.fechaCreacion < :fechaLimite
			""")
	List<Pedido> findPendientesVencidosConDetalle(EstadoPedido estadoPedido, LocalDateTime fechaLimite);
}
