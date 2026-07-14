package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_tienda")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionTienda {

	public static final Long CONFIG_ID = 1L;
	public static final String DEFAULT_EMAIL = "info@electrodentalnea.com";
	public static final String DEFAULT_LOGO_URL = "/img/electrodentallarge.png";
	public static final String DEFAULT_FONDO_URL = "/img/arg.png";

	@Id
	private Long id = CONFIG_ID;

	@Column(length = 40)
	private String whatsapp = "3624541102";

	@Column(length = 320)
	private String email = DEFAULT_EMAIL;

	@Column(length = 220)
	private String direccion = "Roque Sáenz Peña 539, Resistencia, Chaco";

	@Column(length = 20)
	private String codigoPostal = "3500";

	@Column(length = 120)
	private String ciudad = "Resistencia";

	@Column(length = 220)
	private String instagram = "https://www.instagram.com/electrodental_nea/";

	@Column(length = 220)
	private String facebook = "https://www.facebook.com/electrodental.nea";

	@Column(length = 120)
	private String horarios = "08:00 a 12:30hs y 17:00 a 20:30hs";

	@Column(length = 600)
	private String mapsEmbedUrl = "https://www.google.com/maps?q=Roque%20S%C3%A1enz%20Pe%C3%B1a%20539%20Resistencia%20Chaco&output=embed";

	@Column(precision = 14, scale = 2)
	private BigDecimal montoEnvioGratis = BigDecimal.ZERO;

	@Column(nullable = false)
	private Boolean paginaOculta = false;

	@Column(length = 320)
	private String logoUrl = DEFAULT_LOGO_URL;

	@Column(length = 320)
	private String fondoUrl = DEFAULT_FONDO_URL;

	@Column(length = 120)
	private String transferenciaBanco = "";

	@Column(length = 160)
	private String transferenciaTitular = "";

	@Column(length = 40)
	private String transferenciaCbu = "";

	@Column(length = 80)
	private String transferenciaAlias = "";

	@Column(length = 40)
	private String transferenciaCuit = "";

	@Column(length = 80)
	private String adminUsuario = "admin";

	@Column(length = 120)
	private String adminPasswordHash;

	@PrePersist
	public void asegurarId() {
		if (id == null) {
			id = CONFIG_ID;
		}
		completarDefaults();
	}

	@PreUpdate
	@PostLoad
	public void completarDefaults() {
		if (whatsapp == null || whatsapp.isBlank()) {
			whatsapp = "3624541102";
		}
		if (email == null || email.isBlank()) {
			email = DEFAULT_EMAIL;
		} else {
			email = normalizarEmails(email);
		}
		if (direccion == null || direccion.isBlank()) {
			direccion = "Roque Sáenz Peña 539, Resistencia, Chaco";
		}
		if (codigoPostal == null || codigoPostal.isBlank()) {
			codigoPostal = "3500";
		}
		if (ciudad == null || ciudad.isBlank()) {
			ciudad = "Resistencia";
		}
		if (horarios == null || horarios.isBlank()) {
			horarios = "08:00 a 12:30hs y 17:00 a 20:30hs";
		}
		if (adminUsuario == null || adminUsuario.isBlank()) {
			adminUsuario = "admin";
		}
		if (montoEnvioGratis == null || montoEnvioGratis.compareTo(BigDecimal.ZERO) < 0) {
			montoEnvioGratis = BigDecimal.ZERO;
		}
		if (paginaOculta == null) {
			paginaOculta = false;
		}
		if (logoUrl == null || logoUrl.isBlank()) {
			logoUrl = DEFAULT_LOGO_URL;
		}
		if (fondoUrl == null) {
			fondoUrl = DEFAULT_FONDO_URL;
		} else {
			fondoUrl = limpiarTexto(fondoUrl);
		}
		transferenciaBanco = limpiarTexto(transferenciaBanco);
		transferenciaTitular = limpiarTexto(transferenciaTitular);
		transferenciaCbu = limpiarTexto(transferenciaCbu);
		transferenciaAlias = limpiarTexto(transferenciaAlias);
		transferenciaCuit = limpiarTexto(transferenciaCuit);
	}

	public boolean paginaOcultaActiva() {
		return Boolean.TRUE.equals(paginaOculta);
	}

	public boolean datosBancariosConfigurados() {
		return !transferenciaTitular.isBlank()
				&& (!transferenciaAlias.isBlank() || !transferenciaCbu.isBlank());
	}

	public boolean fondoConfigurado() {
		return fondoUrl != null && !fondoUrl.isBlank();
	}

	public String getFondoMainStyle() {
		return fondoConfigurado()
				? "--public-main-background: url('" + fondoUrl + "')"
				: "";
	}

	public String getWhatsappLink() {
		String numero = whatsapp == null ? "" : whatsapp.replaceAll("[^0-9]", "");
		if (!numero.startsWith("54")) {
			numero = "54" + numero;
		}
		return "https://wa.me/" + numero;
	}

	public void setEmail(String email) {
		this.email = normalizarEmails(email);
	}

	public String getEmailMailto() {
		return "mailto:" + String.join(",", getEmailList());
	}

	public List<String> getEmailList() {
		String emails = normalizarEmails(email);
		if (emails.isBlank()) {
			return List.of(DEFAULT_EMAIL);
		}
		return Arrays.stream(emails.split(","))
				.map(String::trim)
				.filter(email -> !email.isBlank())
				.toList();
	}

	public boolean envioGratisHabilitado() {
		return montoEnvioGratis != null && montoEnvioGratis.compareTo(BigDecimal.ZERO) > 0;
	}

	public boolean alcanzaEnvioGratis(BigDecimal subtotal) {
		return envioGratisHabilitado()
				&& subtotal != null
				&& subtotal.compareTo(montoEnvioGratis) >= 0;
	}

	public BigDecimal faltanteEnvioGratis(BigDecimal subtotal) {
		if (!envioGratisHabilitado()) {
			return BigDecimal.ZERO;
		}
		BigDecimal faltante = montoEnvioGratis.subtract(subtotal == null ? BigDecimal.ZERO : subtotal);
		return faltante.compareTo(BigDecimal.ZERO) > 0 ? faltante : BigDecimal.ZERO;
	}

	private static String normalizarEmails(String emails) {
		if (emails == null) {
			return "";
		}
		return Arrays.stream(emails.split("[,;\\s]+"))
				.map(String::trim)
				.filter(email -> !email.isBlank())
				.distinct()
				.collect(Collectors.joining(", "));
	}

	private static String limpiarTexto(String texto) {
		return texto == null ? "" : texto.trim();
	}
}
