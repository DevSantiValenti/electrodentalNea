package com.analistas.electrodental.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyFormatterTest {

	private final MoneyFormatter formatter = new MoneyFormatter();

	@Test
	void usaSeparadoresArgentinos() {
		assertThat(formatter.format(new BigDecimal("3900000"))).isEqualTo("$ 3.900.000,00");
		assertThat(formatter.format(new BigDecimal("4750000.5"))).isEqualTo("$ 4.750.000,50");
	}
}
