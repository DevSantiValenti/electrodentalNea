package com.analistas.electrodental.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.Cliente;

public interface IClienteRepository extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findByEmailIgnoreCase(String email);
}
