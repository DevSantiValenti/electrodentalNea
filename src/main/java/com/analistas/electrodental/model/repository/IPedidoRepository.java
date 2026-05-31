package com.analistas.electrodental.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.Pedido;

public interface IPedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findTop10ByOrderByFechaCreacionDesc();

	@EntityGraph(attributePaths = { "cliente", "direccionEnvio", "items", "items.producto", "pago", "envio" })
	Optional<Pedido> findDetalleById(Long id);

	long countByFechaCreacionBetween(LocalDateTime desde, LocalDateTime hasta);

	long countByEstadoPedido(EstadoPedido estadoPedido);
}
