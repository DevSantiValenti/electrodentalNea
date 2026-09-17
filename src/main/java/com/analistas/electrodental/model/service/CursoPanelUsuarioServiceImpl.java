package com.analistas.electrodental.model.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.domain.CursoPanelUsuario;
import com.analistas.electrodental.model.repository.ICursoPanelUsuarioRepository;

@Service
@Transactional(readOnly = true)
public class CursoPanelUsuarioServiceImpl implements ICursoPanelUsuarioService {

	private final ICursoPanelUsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public CursoPanelUsuarioServiceImpl(
			ICursoPanelUsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public List<CursoPanelUsuario> listarTodos() {
		return usuarioRepository.findAll().stream()
				.sorted(Comparator.comparing(
						CursoPanelUsuario::getUsuario,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
				.toList();
	}

	@Override
	public Optional<CursoPanelUsuario> buscarPorId(Long id) {
		return usuarioRepository.findById(id);
	}

	@Override
	@Transactional
	public CursoPanelUsuario guardar(CursoPanelUsuario usuario, String passwordPlano) {
		CursoPanelUsuario destino = usuario.getId() == null
				? new CursoPanelUsuario()
				: usuarioRepository.findById(usuario.getId())
						.orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + usuario.getId()));
		destino.setNombre(usuario.getNombre());
		destino.setUsuario(usuario.getUsuario());
		destino.setActivo(Boolean.TRUE.equals(usuario.getActivo()));
		if (StringUtils.hasText(passwordPlano)) {
			destino.setPasswordHash(passwordEncoder.encode(passwordPlano.trim()));
		}
		destino.completarDefaults();
		return usuarioRepository.save(destino);
	}

	@Override
	@Transactional
	public void desactivar(Long id) {
		usuarioRepository.findById(id).ifPresent(usuario -> {
			usuario.setActivo(false);
			usuarioRepository.save(usuario);
		});
	}
}
