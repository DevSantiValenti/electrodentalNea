package com.analistas.electrodental.web.view;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component("money")
public class MoneyFormatter {

	private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

	public String format(Number value) {
		NumberFormat formatter = NumberFormat.getNumberInstance(ARGENTINA);
		formatter.setGroupingUsed(true);
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
		return "$ " + formatter.format(value == null ? BigDecimal.ZERO : value);
	}
}
