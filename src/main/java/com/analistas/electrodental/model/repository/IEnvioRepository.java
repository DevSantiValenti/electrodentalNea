package com.analistas.electrodental.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.Envio;

public interface IEnvioRepository extends JpaRepository<Envio, Long> {

	Optional<Envio> findByPedidoId(Long pedidoId);
}
