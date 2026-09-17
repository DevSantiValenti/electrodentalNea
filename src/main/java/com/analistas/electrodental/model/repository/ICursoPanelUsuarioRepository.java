package com.analistas.electrodental.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analistas.electrodental.model.domain.CursoPanelUsuario;

public interface ICursoPanelUsuarioRepository extends JpaRepository<CursoPanelUsuario, Long> {

	Optional<CursoPanelUsuario> findByUsuarioIgnoreCase(String usuario);
}
