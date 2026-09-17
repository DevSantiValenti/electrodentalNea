package com.analistas.electrodental.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SiteControllerRenderTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void homeRenderizaSinErroresDeTemplate() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"));
	}

	@Test
	void robotsTxtDevuelveTextoValido() throws Exception {
		mockMvc.perform(get("/robots.txt"))
				.andExpect(status().isOk())
				.andExpect(result -> Assertions.assertThat(result.getResponse().getContentAsString())
						.contains("User-agent: *")
						.contains("Sitemap: https://electrodentalnea.com.ar/sitemap.xml")
						.doesNotContain("<!DOCTYPE html>"));
	}

	@Test
	void chromeDevToolsProbeNoGeneraVistaDeError() throws Exception {
		mockMvc.perform(get("/.well-known/appspecific/com.chrome.devtools.json"))
				.andExpect(status().isNoContent());
	}

	@Test
	void sitemapXmlDevuelveXmlValido() throws Exception {
		mockMvc.perform(get("/sitemap.xml"))
				.andExpect(status().isOk())
				.andExpect(result -> Assertions.assertThat(result.getResponse().getContentAsString())
						.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
						.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
						.contains("<loc>https://electrodentalnea.com.ar/</loc>"));
	}

	@Test
	void cursosRenderizaListadoPublico() throws Exception {
		mockMvc.perform(get("/cursos"))
				.andExpect(status().isOk())
				.andExpect(view().name("cursos/listado"));
	}

	@Test
	void cursoInexistenteRenderizaDetalleSinError() throws Exception {
		mockMvc.perform(get("/cursos/no-existe"))
				.andExpect(status().isOk())
				.andExpect(view().name("cursos/detalle"));
	}

	@Test
	void claseInexistenteRenderizaSinError() throws Exception {
		mockMvc.perform(get("/cursos/no-existe/clases/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("cursos/clase"));
	}

	@Test
	void panelCursosRenderizaSinErroresDeTemplate() throws Exception {
		mockMvc.perform(get("/admin/cursos-panel"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/cursos-panel/dashboard"));
	}

	@Test
	void formularioNuevoCursoRenderizaSinErroresDeTemplate() throws Exception {
		mockMvc.perform(get("/admin/cursos-panel/cursos/nuevo"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/cursos-panel/curso-form"));
	}

	@Test
	void cuentasCursosRenderizaSinErroresDeTemplate() throws Exception {
		mockMvc.perform(get("/admin/cuentas-cursos"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/cuentas-cursos"));
	}

	@Test
	void formularioCuentaCursosRenderizaSinErroresDeTemplate() throws Exception {
		mockMvc.perform(get("/admin/cuentas-cursos/nueva"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/cuenta-curso-form"));
	}
}
