package com.analistas.electrodental.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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

	@Id
	private Long id = CONFIG_ID;

	@Column(length = 40)
	private String whatsapp = "3624541102";

	@Column(length = 160)
	private String email = "info@electrodentalnea.com";

	@Column(length = 220)
	private String direccion = "Roque Sáenz Peña 539, Resistencia, Chaco";

	@Column(length = 220)
	private String instagram = "https://www.instagram.com/electrodental_nea/";

	@Column(length = 220)
	private String facebook = "https://www.facebook.com/electrodental.nea";

	@Column(length = 120)
	private String horarios = "08:00 a 12:30hs y 17:00 a 20:30hs";

	@Column(length = 600)
	private String mapsEmbedUrl = "https://www.google.com/maps?q=Roque%20S%C3%A1enz%20Pe%C3%B1a%20539%20Resistencia%20Chaco&output=embed";

	@PrePersist
	public void asegurarId() {
		if (id == null) {
			id = CONFIG_ID;
		}
	}

	public String getWhatsappLink() {
		String numero = whatsapp == null ? "" : whatsapp.replaceAll("[^0-9]", "");
		if (!numero.startsWith("54")) {
			numero = "54" + numero;
		}
		return "https://wa.me/" + numero;
	}
}
