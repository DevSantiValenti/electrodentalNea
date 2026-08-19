package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.util.MultiValueMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.analistas.electrodental.model.domain.Envio;
import com.analistas.electrodental.model.domain.EstadoEnvio;
import com.analistas.electrodental.web.config.OcaProperties;

class OcaServiceImplTest {

	private OcaServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OcaServiceImpl(new OcaProperties(), null);
	}

	@Test
	void mapearEstadoOcaReconoceCodigoEstadoWebOcaEntregado() throws Exception {
		assertThat(mapearEstado("9")).contains(EstadoEnvio.ENTREGADO);
		assertThat(mapearEstado("CodigoEstadoWebOCA 9 EstadoWebOCA Entregado")).contains(EstadoEnvio.ENTREGADO);
		assertThat(mapearEstado("Visita Entregada")).contains(EstadoEnvio.ENTREGADO);
	}

	@Test
	void buscarEstadoEnvioEnListadoUsaElContenedorDelEnvioYNoElTracking() throws Exception {
		Envio envio = new Envio();
		envio.setNumeroOrdenRetiro("146854682");
		envio.setNumeroEnvio("9347900000000000009");
		envio.setTracking("9347900000000000009");
		Document document = document("""
				<NewDataSet>
					<Table>
						<NroEnvio>9347900000000000009</NroEnvio>
						<idOrdenRetiro>146854682</idOrdenRetiro>
						<CodigoEstadoWebOCA>9</CodigoEstadoWebOCA>
						<EstadoWebOCA>Entregado </EstadoWebOCA>
						<DescripcionEstadoMotivoUnificado>Entregada</DescripcionEstadoMotivoUnificado>
					</Table>
				</NewDataSet>
				""");

		String estado = buscarEstado(document, envio);

		assertThat(estado).contains("9", "Entregado", "Entregada");
		assertThat(mapearEstado(estado)).contains(EstadoEnvio.ENTREGADO);
	}

	@Test
	void estadoDesdeRespuestaActualNoNecesitaQueOcaRepitaElTracking() throws Exception {
		Document document = document("""
				<NewDataSet>
					<Table>
						<CodigoEstadoWebOCA>9</CodigoEstadoWebOCA>
						<EstadoWebOCA>Entregado </EstadoWebOCA>
						<DescripcionEstadoMotivoUnificado>Entregada</DescripcionEstadoMotivoUnificado>
					</Table>
				</NewDataSet>
				""");

		String estado = estadoDesdeRespuesta(document);

		assertThat(estado).contains("9", "Entregado", "Entregada");
		assertThat(mapearEstado(estado)).contains(EstadoEnvio.ENTREGADO);
	}

	@Test
	void formEstadoActualUsaNumeroEnvioYOrdenRetiroCanonicos() throws Exception {
		Envio envio = new Envio();
		envio.setNumeroEnvio("9347900000000000009");
		envio.setNumeroOrdenRetiro("146854682");

		MultiValueMap<String, String> form = formEstadoActual(envio, "AMBOS");

		assertThat(form.getFirst("numeroEnvio")).isEqualTo("9347900000000000009");
		assertThat(form.getFirst("ordenRetiro")).isEqualTo("146854682");
	}

	@Test
	void formListadoEnviosEnviaCuitConGuionesYFechasDdMmAaaa() throws Exception {
		OcaProperties properties = new OcaProperties();
		properties.setCuit("30536259194");
		service = new OcaServiceImpl(properties, null);

		MultiValueMap<String, String> form = formListadoEnvios(
				LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 8, 3));

		assertThat(form.getFirst("CUIT")).isEqualTo("30-53625919-4");
		assertThat(form.getFirst("FechaDesde")).isEqualTo("01-07-2026");
		assertThat(form.getFirst("FechaHasta")).isEqualTo("03-08-2026");
	}

	@SuppressWarnings("unchecked")
	private Optional<EstadoEnvio> mapearEstado(String estado) throws Exception {
		Method method = OcaServiceImpl.class.getDeclaredMethod("mapearEstadoOca", String.class);
		method.setAccessible(true);
		return (Optional<EstadoEnvio>) method.invoke(service, estado);
	}

	private String buscarEstado(Document document, Envio envio) throws Exception {
		Method method = OcaServiceImpl.class.getDeclaredMethod("buscarEstadoEnvioEnListado", Document.class, Envio.class);
		method.setAccessible(true);
		return (String) method.invoke(service, document, envio);
	}

	private String estadoDesdeRespuesta(Document document) throws Exception {
		Method method = OcaServiceImpl.class.getDeclaredMethod("estadoDesdeRespuesta", Document.class);
		method.setAccessible(true);
		return (String) method.invoke(service, document);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> formEstadoActual(Envio envio, String tipoBusqueda) throws Exception {
		Class<?> tipo = Class.forName("com.analistas.electrodental.model.service.OcaServiceImpl$TipoBusquedaEtiqueta");
		Object enumValue = Enum.valueOf((Class<Enum>) tipo.asSubclass(Enum.class), tipoBusqueda);
		Method method = OcaServiceImpl.class.getDeclaredMethod("formEstadoActual", Envio.class, tipo);
		method.setAccessible(true);
		return (MultiValueMap<String, String>) method.invoke(service, envio, enumValue);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> formListadoEnvios(LocalDate fechaDesde, LocalDate fechaHasta) throws Exception {
		Method method = OcaServiceImpl.class.getDeclaredMethod("formListadoEnvios", LocalDate.class, LocalDate.class);
		method.setAccessible(true);
		return (MultiValueMap<String, String>) method.invoke(service, fechaDesde, fechaHasta);
	}

	private Document document(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}
}
