package com.analistas.electrodental.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.IProductoService;

@RestController
public class SeoController {

	private static final String BASE_URL = "https://electrodentalnea.com.ar";

	private final IProductoService productoService;
	private final ICategoriaService categoriaService;

	public SeoController(IProductoService productoService, ICategoriaService categoriaService) {
		this.productoService = productoService;
		this.categoriaService = categoriaService;
	}

	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public String robotsTxt() {
		return """
				User-agent: *
				Allow: /
				Disallow: /admin/
				Disallow: /panel
				Disallow: /api/
				Disallow: /carrito
				Disallow: /checkout
				Disallow: /finalizar-compra

				Sitemap: https://electrodentalnea.com.ar/sitemap.xml
				""";
	}

	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String sitemapXml() {
		List<SitemapEntry> entries = new ArrayList<>();
		entries.add(new SitemapEntry("/", "daily", "1.0"));
		entries.add(new SitemapEntry("/catalogo", "daily", "0.9"));
		entries.add(new SitemapEntry("/ofertas", "daily", "0.8"));
		entries.add(new SitemapEntry("/servicio-tecnico", "monthly", "0.6"));
		entries.add(new SitemapEntry("/contacto", "monthly", "0.5"));

		for (Categoria categoria : categoriaService.listarActivas()) {
			if (StringUtils.hasText(categoria.getSlug())) {
				entries.add(new SitemapEntry("/catalogo/categoria/" + categoria.getSlug(), "weekly", "0.8"));
				for (Subcategoria subcategoria : categoriaService.listarSubcategoriasActivasPorCategoria(categoria.getId())) {
					if (StringUtils.hasText(subcategoria.getSlug())) {
						entries.add(new SitemapEntry("/catalogo/subcategoria/" + subcategoria.getSlug(), "weekly", "0.7"));
					}
				}
			}
		}

		for (Producto producto : productoService.listarActivos()) {
			if (StringUtils.hasText(producto.getSlug())) {
				entries.add(new SitemapEntry("/productos/" + producto.getSlug(), "weekly", "0.7"));
			}
		}

		StringBuilder sitemap = new StringBuilder();
		sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		for (SitemapEntry entry : entries) {
			sitemap.append("  <url>\n");
			sitemap.append("    <loc>").append(escape(BASE_URL + entry.path())).append("</loc>\n");
			sitemap.append("    <changefreq>").append(entry.changefreq()).append("</changefreq>\n");
			sitemap.append("    <priority>").append(entry.priority()).append("</priority>\n");
			sitemap.append("  </url>\n");
		}
		sitemap.append("</urlset>\n");
		return sitemap.toString();
	}

	private String escape(String value) {
		return HtmlUtils.htmlEscape(value);
	}

	private record SitemapEntry(String path, String changefreq, String priority) {
	}
}
