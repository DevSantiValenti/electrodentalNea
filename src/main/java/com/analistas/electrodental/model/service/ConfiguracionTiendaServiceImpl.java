package com.analistas.electrodental.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.repository.IConfiguracionTiendaRepository;

@Service
@Transactional(readOnly = true)
public class ConfiguracionTiendaServiceImpl implements IConfiguracionTiendaService {

	private final IConfiguracionTiendaRepository configuracionRepository;
	private final PasswordEncoder passwordEncoder;

	public ConfiguracionTiendaServiceImpl(
			IConfiguracionTiendaRepository configuracionRepository,
			PasswordEncoder passwordEncoder) {
		this.configuracionRepository = configuracionRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public ConfiguracionTienda obtener() {
		ConfiguracionTienda configuracion = configuracionRepository.findById(ConfiguracionTienda.CONFIG_ID)
				.orElseGet(ConfiguracionTienda::new);
		configuracion.completarDefaults();
		return configuracion;
	}

	@Override
	@Transactional
	public ConfiguracionTienda guardar(ConfiguracionTienda configuracion) {
		return guardar(configuracion, null);
	}

	@Override
	@Transactional
	public ConfiguracionTienda guardar(ConfiguracionTienda configuracion, String adminPassword) {
		ConfiguracionTienda actual = configuracionRepository.findById(ConfiguracionTienda.CONFIG_ID)
				.orElseGet(ConfiguracionTienda::new);
		configuracion.setId(ConfiguracionTienda.CONFIG_ID);
		if (!StringUtils.hasText(configuracion.getAdminUsuario())) {
			configuracion.setAdminUsuario(actual.getAdminUsuario());
		}
		configuracion.setAdminPasswordHash(actual.getAdminPasswordHash());
		if (StringUtils.hasText(adminPassword)) {
			configuracion.setAdminPasswordHash(passwordEncoder.encode(adminPassword.trim()));
		}
		configuracion.completarDefaults();
		return configuracionRepository.save(configuracion);
	}
}
