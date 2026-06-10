package com.analistas.electrodental.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.Pedido;

public interface IPedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findTop10ByOrderByFechaCreacionDesc();

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
