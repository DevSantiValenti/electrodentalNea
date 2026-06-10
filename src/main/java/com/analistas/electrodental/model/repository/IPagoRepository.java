package com.analistas.electrodental.model.repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.analistas.electrodental.model.domain.EstadoPago;
import com.analistas.electrodental.model.domain.Pago;

public interface IPagoRepository extends JpaRepository<Pago, Long> {

	Optional<Pago> findByExternalReference(String externalReference);

	Optional<Pago> findByPaymentId(String paymentId);

	@Query("""
			select p from Pago p
			join fetch p.pedido pedido
			where p.estadoPago = :estado
			  and p.paymentId is not null
			  and pedido.fechaCreacion >= :desde
			""")
	List<Pago> findPendientesRecientesConPaymentId(EstadoPago estado, LocalDateTime desde);
}
