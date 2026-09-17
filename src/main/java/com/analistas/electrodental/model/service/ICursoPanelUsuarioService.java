package com.analistas.electrodental.model.service;

import java.util.List;
import java.util.Optional;

import com.analistas.electrodental.model.domain.CursoPanelUsuario;

public interface ICursoPanelUsuarioService {

	List<CursoPanelUsuario> listarTodos();

	Optional<CursoPanelUsuario> buscarPorId(Long id);

	CursoPanelUsuario guardar(CursoPanelUsuario usuario, String passwordPlano);

	void desactivar(Long id);
}
