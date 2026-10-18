package com.analistas.electrodental.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import com.analistas.electrodental.model.domain.*;
import com.analistas.electrodental.model.service.*;

class SeoCanonicalTest {
    private static final String ORIGIN = "https://electrodentalnea.com.ar";
    private IProductoService productos;
    private ICategoriaService categorias;
    private ICursoService cursos;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        productos = mock(IProductoService.class);
        categorias = mock(ICategoriaService.class);
        cursos = mock(ICursoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SiteController(productos, categorias),
                new CursoPublicController(cursos, mock(ICursoAreaService.class), productos,
                        mock(ProductoImagenStorageService.class)))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @Test
    void aliasesPublicosCompartenCanonicaSinRedirigir() throws Exception {
        for (String path : List.of("/", "/inicio")) {
            mvc.perform(get(path).param("utm_source", "google").header("Host", "www.electrodentalnea.com.ar"))
                .andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(model().attribute("seoCanonical", ORIGIN + "/"));
        }
        for (String path : List.of("/catalogo", "/productos")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(view().name("catalogo"))
                .andExpect(model().attribute("seoCanonical", ORIGIN + "/catalogo"));
        }
        for (String path : List.of("/cursos", "/capacitaciones")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(view().name("cursos/listado"))
                .andExpect(model().attribute("seoCanonical", ORIGIN + "/cursos"));
        }
        mvc.perform(get("/ofertas")).andExpect(model().attribute("seoCanonical", ORIGIN + "/ofertas"));
        mvc.perform(get("/contacto")).andExpect(model().attribute("seoCanonical", ORIGIN + "/contacto"));
		mvc.perform(get("/politica-privacidad")).andExpect(model().attribute("seoCanonical", ORIGIN + "/politica-privacidad"));
		mvc.perform(get("/politica-devoluciones")).andExpect(model().attribute("seoCanonical", ORIGIN + "/politica-devoluciones"));
		mvc.perform(get("/politica-devolucion")).andExpect(model().attribute("seoCanonical", ORIGIN + "/politica-devoluciones"));
		mvc.perform(get("/politica-publicidad")).andExpect(model().attribute("seoCanonical", ORIGIN + "/politica-publicidad"));
    }

    @Test
    void parametrosDeCategoriaUsanSlugGuardadoYRespetanLaSubcategoria() throws Exception {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setSlug("compresores");
        when(categorias.buscarCategoriaPorSlug("COMPRESORES")).thenReturn(Optional.of(categoria));
        mvc.perform(get("/catalogo").param("categoria", "COMPRESORES"))
            .andExpect(model().attribute("seoCanonical", ORIGIN + "/catalogo/categoria/compresores"));
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setSlug("odontologicos");
        subcategoria.setCategoria(categoria);
        when(categorias.buscarSubcategoriaPorSlug("ODONTOLOGICOS")).thenReturn(Optional.of(subcategoria));
        for (String path : List.of("/catalogo", "/productos")) {
            mvc.perform(get(path).param("categoria", "COMPRESORES").param("subcategoria", "ODONTOLOGICOS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("seoCanonical", ORIGIN + "/catalogo/subcategoria/odontologicos"));
        }
        mvc.perform(get("/catalogo/subcategoria/ODONTOLOGICOS"))
            .andExpect(model().attribute("seoCanonical", ORIGIN + "/catalogo/subcategoria/odontologicos"));
    }

    @Test
    void filtrosConservanContenidoYCaracteresEspeciales() throws Exception {
        Producto producto = new Producto();
        producto.setNombre("Kit A+B & niños");
        producto.setMarca("Marca");
        producto.setPrecio(new BigDecimal("15"));
        when(productos.listarActivos()).thenReturn(List.of(producto));
        when(productos.listarPorCategoria("kits")).thenReturn(List.of(producto));
        mvc.perform(get("/catalogo").param("categoria", "kits").param("q", "A+B"))
            .andExpect(model().attribute("productos", List.of(producto)))
            .andExpect(model().attribute("seoCanonical", ORIGIN + "/catalogo?categoria=kits&q=A%2BB"));
        mvc.perform(get("/catalogo").param("q", "A+B & niños").param("marca", "Marca")
                .param("precioMin", "10.00").param("precioMax", "20").param("utm_source", "google"))
            .andExpect(status().isOk()).andExpect(model().attribute("productos", List.of(producto)))
            .andExpect(model().attribute("seoCanonical", ORIGIN + "/catalogo?q=A%2BB%20%26%20ni%C3%B1os&marca=Marca&precioMin=10&precioMax=20"));
    }

    @Test
    void productoUsaSlugPersistidoSinModificarloNiCambiarVista() throws Exception {
        Producto producto = new Producto();
        producto.setSlug("x3 colibrí");
        producto.setNombre("X3");
        when(productos.buscarPorSlug("X3 COLIBRÍ")).thenReturn(Optional.of(producto));
        for (String path : List.of("/producto/{slug}", "/productos/{slug}")) {
            mvc.perform(get(path, "X3 COLIBRÍ")).andExpect(status().isOk())
                .andExpect(view().name("producto")).andExpect(model().attribute("producto", producto))
                .andExpect(model().attribute("seoCanonical", ORIGIN + "/productos/x3%20colibr%C3%AD"));
        }
        assertThat(producto.getSlug()).isEqualTo("x3 colibrí");
        mvc.perform(get("/productos/inexistente")).andExpect(view().name("producto"))
            .andExpect(model().attributeDoesNotExist("seoCanonical"));
    }

    @Test
    void cursoAliasUsaCanonicaDelCursoSinAlterarCertificados() throws Exception {
        Curso curso = new Curso();
        curso.setSlug("curso-prueba");
        curso.setTitulo("Curso prueba");
        when(cursos.buscarActivoPorSlug("curso-prueba")).thenReturn(Optional.of(curso));
        for (String path : List.of("/cursos/curso-prueba", "/capacitaciones/curso-prueba")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(view().name("cursos/detalle"))
                .andExpect(model().attribute("seoCanonical", ORIGIN + "/cursos/curso-prueba"));
        }
    }

    @Test
    void carritoNoRecibeCanonicaPublica() throws Exception {
        for (String path : List.of("/carrito", "/checkout", "/finalizar-compra")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(view().name("finalizar-compra"))
                .andExpect(model().attributeDoesNotExist("seoCanonical"));
        }
    }

    @Test
    void headRenderizadoSoloAgregaEtiquetaInvisibleYEscapaUrl() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        var response = new MockHttpServletResponse();
        var exchange = JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response);
        var context = new WebContext(exchange);
        context.setVariables(Map.of("title", "Catálogo", "assetVersion", "test",
                "configuracionTienda", new ConfiguracionTienda()));
        String before = engine.process("fragments/shop", Set.of("head"), context);
        assertThat(before).doesNotContain("rel=\"canonical\"");
        context.setVariable("seoCanonical", ORIGIN + "/catalogo?q=kit&marca=Marca");
        String after = engine.process("fragments/shop", Set.of("head"), context);
        assertThat(after).containsOnlyOnce("rel=\"canonical\"")
                .contains("href=\"" + ORIGIN + "/catalogo?q=kit&amp;marca=Marca\"");
        assertThat(after.replaceAll("<link[^>]*rel=\"canonical\"[^>]*>", "")).isEqualTo(before);
        String serviceHead = engine.process("servicio-tecnico", Set.of("head"), context);
        assertThat(serviceHead).containsOnlyOnce("rel=\"canonical\"")
                .contains("href=\"" + ORIGIN + "/servicio-tecnico\"");
    }
}
