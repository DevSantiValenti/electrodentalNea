package com.analistas.electrodental.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.repository.IConfiguracionTiendaRepository;

@Service
@Transactional(readOnly = true)
public class ConfiguracionTiendaServiceImpl implements IConfiguracionTiendaService {

	private final IConfiguracionTiendaRepository configuracionRepository;

	public ConfiguracionTiendaServiceImpl(IConfiguracionTiendaRepository configuracionRepository) {
		this.configuracionRepository = configuracionRepository;
	}

	@Override
	public ConfiguracionTienda obtener() {
		return configuracionRepository.findById(ConfiguracionTienda.CONFIG_ID)
				.orElseGet(ConfiguracionTienda::new);
	}

	@Override
	@Transactional
	public ConfiguracionTienda guardar(ConfiguracionTienda configuracion) {
		configuracion.setId(ConfiguracionTienda.CONFIG_ID);
		return configuracionRepository.save(configuracion);
	}
}
