package com.analistas.electrodental.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.Pago;

public interface IPagoRepository extends JpaRepository<Pago, Long> {

	Optional<Pago> findByExternalReference(String externalReference);

	Optional<Pago> findByPaymentId(String paymentId);
}
