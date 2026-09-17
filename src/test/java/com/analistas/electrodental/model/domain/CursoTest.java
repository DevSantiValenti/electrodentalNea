package com.analistas.electrodental.model.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CursoTest {

	@Test
	void codigoDeCertificadoIgnoraMayusculasYEspacios() {
		Curso curso = new Curso();
		curso.setCertificadoModo(CertificadoModo.CODIGO);
		curso.setCertificadoCodigo(" ab 123 ");
		curso.completarDefaults();

		assertThat(curso.codigoCertificadoValido("AB123")).isTrue();
		assertThat(curso.codigoCertificadoValido(" ab 123 ")).isTrue();
		assertThat(curso.codigoCertificadoValido("otro")).isFalse();
	}

	@Test
	void clasesOrdenadasActivasOmiteInactivas() {
		Curso curso = new Curso();
		curso.agregarClase(clase("Segunda", 2, true));
		curso.agregarClase(clase("Inactiva", 1, false));
		curso.agregarClase(clase("Primera", 1, true));

		assertThat(curso.getClasesOrdenadasActivas())
				.extracting(CursoClase::getTitulo)
				.containsExactly("Primera", "Segunda");
	}

	@Test
	void youtubeEmbedUrlAceptaWatchShortsYBe() {
		assertThat(claseConUrl("https://www.youtube.com/watch?v=abcDEF_1234").getYoutubeEmbedUrl())
				.isEqualTo("https://www.youtube.com/embed/abcDEF_1234");
		assertThat(claseConUrl("https://youtu.be/abcDEF_1234").getYoutubeEmbedUrl())
				.isEqualTo("https://www.youtube.com/embed/abcDEF_1234");
		assertThat(claseConUrl("https://www.youtube.com/shorts/abcDEF_1234").getYoutubeEmbedUrl())
				.isEqualTo("https://www.youtube.com/embed/abcDEF_1234");
	}

	@Test
	void slugDeCursoSeNormalizaSinAcentos() {
		assertThat(Curso.normalizarSlug("Curso de Tartrectomía"))
				.isEqualTo("curso-de-tartrectomia");
		assertThat(Curso.normalizarSlug("Capacitación Ñandú 2026"))
				.isEqualTo("capacitacion-nandu-2026");
	}

	private CursoClase clase(String titulo, int orden, boolean activo) {
		CursoClase clase = claseConUrl("https://www.youtube.com/watch?v=" + titulo + "123");
		clase.setTitulo(titulo);
		clase.setOrden(orden);
		clase.setActivo(activo);
		return clase;
	}

	private CursoClase claseConUrl(String url) {
		CursoClase clase = new CursoClase();
		clase.setYoutubeUrl(url);
		return clase;
	}
}
