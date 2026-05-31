package com.analistas.electrodental.model.service;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;

public interface IConfiguracionTiendaService {

	ConfiguracionTienda obtener();

	ConfiguracionTienda guardar(ConfiguracionTienda configuracion);
}
