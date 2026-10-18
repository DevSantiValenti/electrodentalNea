package com.analistas.electrodental.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.ICursoService;
import com.analistas.electrodental.model.service.IProductoService;

class SeoControllerTest {

	@Test
	void sitemapEsXmlValidoConAcentosEspaciosYCaracteresEspeciales() throws Exception {
		IProductoService productos = mock(IProductoService.class, withSettings().mockMaker("mock-maker-proxy"));
		ICategoriaService categorias = mock(ICategoriaService.class, withSettings().mockMaker("mock-maker-proxy"));
		ICursoService cursos = mock(ICursoService.class, withSettings().mockMaker("mock-maker-proxy"));
		Producto producto = new Producto();
		producto.setSlug("x3 colibrí & especial");
		when(productos.listarActivos()).thenReturn(List.of(producto));
		when(categorias.listarActivas()).thenReturn(List.of());
		when(cursos.listarActivos()).thenReturn(List.of());

		String xml = new SeoController(productos, categorias, cursos).sitemapXml();
		var factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		var urls = document.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
		assertThat(urls.getLength()).isEqualTo(10);
		assertThat(urls.item(9).getTextContent())
				.isEqualTo("https://electrodentalnea.com.ar/productos/x3%20colibr%C3%AD%20&%20especial");
		assertThat(xml).contains("&amp;").doesNotContain("&iacute;");
	}
}
