package com.analistas.electrodental.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.DireccionEnvio;

public interface IDireccionEnvioRepository extends JpaRepository<DireccionEnvio, Long> {
}
