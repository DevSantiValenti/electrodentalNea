package com.analistas.electrodental.model.domain;

import java.net.URI;
import java.util.Arrays;

import org.springframework.util.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "curso_clases")
@Getter
@Setter
@NoArgsConstructor
public class CursoClase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "curso_id", nullable = false)
	private Curso curso;

	@Column(nullable = false, length = 180)
	private String titulo;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(nullable = false, length = 700)
	private String youtubeUrl;

	private Integer orden = 0;

	private Boolean activo = true;

	@PrePersist
	@PreUpdate
	public void completarDefaults() {
		if (orden == null) {
			orden = 0;
		}
		if (activo == null) {
			activo = true;
		}
	}

	public boolean activoVisible() {
		return Boolean.TRUE.equals(activo);
	}

	public Integer ordenSeguro() {
		return orden == null ? 0 : orden;
	}

	public String tituloSeguro() {
		return titulo == null ? "" : titulo;
	}

	public String getYoutubeEmbedUrl() {
		String videoId = extraerYoutubeId(youtubeUrl);
		return StringUtils.hasText(videoId) ? "https://www.youtube.com/embed/" + videoId : "";
	}

	public static boolean esYoutubeValido(String url) {
		return StringUtils.hasText(extraerYoutubeId(url));
	}

	private static String extraerYoutubeId(String url) {
		if (!StringUtils.hasText(url)) {
			return "";
		}
		try {
			URI uri = URI.create(url.trim());
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
			String path = uri.getPath() == null ? "" : uri.getPath();
			if (host.endsWith("youtu.be")) {
				return limpiarVideoId(path.replaceFirst("^/", ""));
			}
			if (host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")) {
				if (path.startsWith("/embed/")) {
					return limpiarVideoId(path.substring("/embed/".length()));
				}
				if (path.startsWith("/shorts/")) {
					return limpiarVideoId(path.substring("/shorts/".length()));
				}
				String query = uri.getRawQuery();
				if (StringUtils.hasText(query)) {
					return Arrays.stream(query.split("&"))
							.map(param -> param.split("=", 2))
							.filter(partes -> partes.length == 2 && "v".equals(partes[0]))
							.map(partes -> limpiarVideoId(partes[1]))
							.filter(StringUtils::hasText)
							.findFirst()
							.orElse("");
				}
			}
		} catch (IllegalArgumentException ex) {
			return "";
		}
		return "";
	}

	private static String limpiarVideoId(String valor) {
		if (!StringUtils.hasText(valor)) {
			return "";
		}
		String id = valor.split("[/?#&]", 2)[0].trim();
		return id.matches("[A-Za-z0-9_-]{6,}") ? id : "";
	}
}
