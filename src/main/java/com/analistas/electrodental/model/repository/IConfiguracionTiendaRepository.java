package com.analistas.electrodental.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;

public interface IConfiguracionTiendaRepository extends JpaRepository<ConfiguracionTienda, Long> {
}
