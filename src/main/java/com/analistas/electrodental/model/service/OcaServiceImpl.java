package com.analistas.electrodental.model.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.DireccionEnvio;
import com.analistas.electrodental.model.domain.Envio;
import com.analistas.electrodental.model.domain.EstadoEnvio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.PedidoItem;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.ProveedorEnvio;
import com.analistas.electrodental.model.domain.dto.OcaCotizacionRequestDTO;
import com.analistas.electrodental.model.domain.dto.OcaCotizacionResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaCreacionEnvioResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaSincronizacionEnvioResponseDTO;
import com.analistas.electrodental.model.domain.dto.OcaSucursalDTO;
import com.analistas.electrodental.model.repository.IEnvioRepository;
import com.analistas.electrodental.web.config.OcaProperties;

@Service
public class OcaServiceImpl implements IOcaService {

	private static final BigDecimal CM3_EN_M3 = new BigDecimal("1000000");
	private static final DateTimeFormatter FECHA_OCA = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter FECHA_LISTADO_OCA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	private static final String TIPO_DOMICILIO = "DOMICILIO";
	private static final String TIPO_SUCURSAL = "SUCURSAL";

	private enum TipoBusquedaEtiqueta {
		ENVIO,
		ORDEN,
		AMBOS
	}

	private final OcaProperties properties;
	private final IEnvioRepository envioRepository;

	public OcaServiceImpl(OcaProperties properties, IEnvioRepository envioRepository) {
		this.properties = properties;
		this.envioRepository = envioRepository;
	}

	@Override
	public OcaCotizacionResponseDTO cotizar(Pedido pedido) {
		return cotizarDomicilio(pedido);
	}

	@Override
	public OcaCotizacionResponseDTO cotizarDomicilio(Pedido pedido) {
		return cotizarConOperativa(pedido, resolverOperativa(TIPO_DOMICILIO), "domicilio");
	}

	@Override
	public OcaCotizacionResponseDTO cotizarSucursal(Pedido pedido) {
		return cotizarConOperativa(pedido, resolverOperativa(TIPO_SUCURSAL), "sucursal OCA");
	}

	private OcaCotizacionResponseDTO cotizarConOperativa(Pedido pedido, String operativa, String modalidad) {
		OcaCotizacionRequestDTO request = crearRequestCotizacion(pedido, operativa);
		String requestXml = formCotizacionComoTexto(request);

		if (!configuracionCotizacionCompleta(operativa)) {
			return new OcaCotizacionResponseDTO(
					false,
					"OCA",
					BigDecimal.ZERO,
					"ARS",
					"Configura oca.api-url, oca.cuit, la operativa OCA de " + modalidad + " y oca.codigo-postal-origen para cotizar.",
					null,
					requestXml,
					null);
		}

		try {
			String responseXml = RestClient.create()
					.post()
					.uri(endpoint("Tarifar_Envio_Corporativo"))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formCotizacion(request))
					.retrieve()
					.body(String.class);

			Document response = parseXml(responseXml);
			BigDecimal costo = decimalDesdeXml(response, "Total", "Precio").setScale(2, RoundingMode.HALF_UP);
			String plazo = textOfFirst(response, "PlazoEntrega");
			String mensaje = costo.compareTo(BigDecimal.ZERO) > 0
					? "Cotizacion OCA generada"
					: valorConDefault(textOfFirst(response, "Error", "Mensaje", "Descripcion"), "OCA no devolvio una tarifa.");
			return new OcaCotizacionResponseDTO(
					costo.compareTo(BigDecimal.ZERO) > 0,
					"OCA",
					costo,
					"ARS",
					mensaje,
					plazo,
					requestXml,
					responseXml);
		} catch (RestClientResponseException ex) {
			return new OcaCotizacionResponseDTO(false, "OCA", BigDecimal.ZERO, "ARS", ex.getMessage(), null, requestXml, ex.getResponseBodyAsString());
		} catch (RuntimeException ex) {
			return new OcaCotizacionResponseDTO(false, "OCA", BigDecimal.ZERO, "ARS", ex.getMessage(), null, requestXml, null);
		}
	}

	@Override
	public List<OcaSucursalDTO> obtenerSucursales(String codigoPostal) {
		if (!StringUtils.hasText(codigoPostal) || !StringUtils.hasText(properties.getApiUrl())) {
			return List.of();
		}
		try {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("CodigoPostal", soloDigitos(codigoPostal));
			String responseXml = RestClient.create()
					.post()
					.uri(endpoint("GetCentrosImposicionConServiciosByCP"))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(String.class);
			return parseSucursales(parseXml(responseXml)).stream()
					.filter(OcaSucursalDTO::entregaPaquetes)
					.toList();
		} catch (RuntimeException ex) {
			return List.of();
		}
	}

	@Override
	@Transactional
	public Envio guardarCotizacion(Pedido pedido, OcaCotizacionResponseDTO cotizacion) {
		return guardarCotizacion(pedido, cotizacion, TIPO_DOMICILIO, null);
	}

	@Override
	@Transactional
	public Envio guardarCotizacion(Pedido pedido, OcaCotizacionResponseDTO cotizacion, String tipoEntregaOca, OcaSucursalDTO sucursal) {
		Envio envio = obtenerEnvio(pedido);
		String tipo = TIPO_SUCURSAL.equals(tipoEntregaOca) ? TIPO_SUCURSAL : TIPO_DOMICILIO;
		OcaCotizacionRequestDTO request = crearRequestCotizacion(
				pedido,
				resolverOperativa(tipo));
		cargarDatosBase(envio, pedido, request);
		envio.setTipoEntregaOca(tipo);
		envio.setSeguro(false);
		if (TIPO_SUCURSAL.equals(tipo) && sucursal != null) {
			envio.setIdCentroImposicionDestino(sucursal.idCentroImposicion());
			envio.setSucursalDestino(sucursal.nombre());
			envio.setSucursalDestinoDireccion(sucursal.direccionResumen());
			envio.setSucursalDestinoHorario(sucursal.horarioAtencion());
		} else {
			envio.setIdCentroImposicionDestino(null);
			envio.setSucursalDestino(null);
			envio.setSucursalDestinoDireccion(null);
			envio.setSucursalDestinoHorario(null);
		}
		envio.setCostoCotizado(cotizacion == null ? BigDecimal.ZERO : cotizacion.costo());
		envio.setEstadoEnvio(cotizacion != null && cotizacion.cotizada() ? EstadoEnvio.COTIZADO : EstadoEnvio.FALLIDO);
		envio.setFechaCotizacion(LocalDateTime.now());
		envio.setRequestCotizacion(cotizacion == null ? null : cotizacion.requestXml());
		envio.setResponseCotizacion(cotizacion == null ? null : cotizacion.responseXml());
		pedido.setEnvio(envio);
		return envioRepository.save(envio);
	}

	@Override
	@Transactional
	public OcaCreacionEnvioResponseDTO crearEnvio(Pedido pedido) {
		Envio envio = obtenerEnvio(pedido);
		if (StringUtils.hasText(envio.getNumeroOrdenRetiro()) || StringUtils.hasText(envio.getNumeroEnvio())) {
			return new OcaCreacionEnvioResponseDTO(
					true,
					envio.getNumeroOrdenRetiro(),
					envio.getNumeroEnvio(),
					"El envio OCA ya estaba creado.",
					envio.getRequestCreacion(),
					envio.getResponseCreacion());
		}

		String operativa = StringUtils.hasText(envio.getOperativa())
				? envio.getOperativa()
				: resolverOperativa(envio.getTipoEntregaOca());
		OcaCotizacionRequestDTO request = crearRequestCotizacion(pedido, operativa);
		cargarDatosBase(envio, pedido, request);
		envio.setSeguro(false);
		String requestXml = construirXmlEnvio(pedido, request, envio);
		envio.setRequestCreacion(requestXml);

		if (!configuracionCreacionCompleta(operativa)) {
			String mensaje = "Configura oca.usuario, oca.password, oca.numero-cuenta, oca.cuit y la operativa OCA para crear envios.";
			envio.setEstadoEnvio(EstadoEnvio.FALLIDO);
			envio.setResponseCreacion(mensaje);
			envioRepository.save(envio);
			return new OcaCreacionEnvioResponseDTO(false, null, null, mensaje, requestXml, mensaje);
		}

		try {
			String responseXml = RestClient.create()
					.post()
					.uri(endpoint("IngresoORMultiplesRetiros"))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formCreacion(requestXml))
					.retrieve()
					.body(String.class);

			Document response = parseXml(responseXml);
			String orden = textOfFirst(response,
					"idOrdenRetiro",
					"IdOrdenRetiro",
					"NroOrdenRetiro",
					"OrdenRetiro",
					"NroOrden",
					"Orden",
					"IdOrden",
					"NumeroOrden");
			String numeroEnvio = textOfFirst(response,
					"numeroEnvio",
					"NumeroEnvio",
					"NroEnvio",
					"nroEnvio",
					"NroGuia",
					"Guia",
					"Tracking");
			String mensaje = valorConDefault(textOfFirst(response, "Mensaje", "Descripcion", "Resultado", "Error"), "Respuesta recibida de OCA.");
			boolean creado = StringUtils.hasText(orden) || StringUtils.hasText(numeroEnvio);

			envio.setNumeroOrdenRetiro(orden);
			envio.setNumeroEnvio(numeroEnvio);
			envio.setTracking(StringUtils.hasText(numeroEnvio) ? numeroEnvio : orden);
			envio.setEstadoEnvio(creado ? EstadoEnvio.PENDIENTE_DESPACHO : EstadoEnvio.FALLIDO);
			envio.setFechaCreacionEnvio(LocalDateTime.now());
			envio.setResponseCreacion(responseXml);
			pedido.setEnvio(envio);
			envioRepository.save(envio);

			return new OcaCreacionEnvioResponseDTO(creado, orden, numeroEnvio, mensaje, requestXml, responseXml);
		} catch (RestClientResponseException ex) {
			envio.setEstadoEnvio(EstadoEnvio.FALLIDO);
			envio.setFechaCreacionEnvio(LocalDateTime.now());
			envio.setResponseCreacion(ex.getResponseBodyAsString());
			envioRepository.save(envio);
			return new OcaCreacionEnvioResponseDTO(false, null, null, ex.getMessage(), requestXml, ex.getResponseBodyAsString());
		} catch (RuntimeException ex) {
			envio.setEstadoEnvio(EstadoEnvio.FALLIDO);
			envio.setFechaCreacionEnvio(LocalDateTime.now());
			envio.setResponseCreacion(ex.getMessage());
			envioRepository.save(envio);
			return new OcaCreacionEnvioResponseDTO(false, null, null, ex.getMessage(), requestXml, ex.getMessage());
		}
	}

	@Override
	@Transactional
	public byte[] obtenerEtiquetaPdf(Pedido pedido) {
		Envio envio = asegurarEnvioCreado(pedido);
		RuntimeException primerError = null;

		for (String operacion : List.of(
				"GetPdfDeEtiquetasPorOrdenOrNumeroEnvio",
				"GetPdfDeEtiquetasPorOrdenOrNumeroEnvioParaEtiquetadora")) {
			for (TipoBusquedaEtiqueta tipoBusqueda : tiposBusquedaEtiqueta(envio)) {
				try {
					String responseXml = solicitarEtiqueta(envio, operacion, tipoBusqueda);
					String base64 = textOfFirst(parseXml(responseXml), "string", operacion + "Result");
					if (!StringUtils.hasText(base64)) {
						continue;
					}
					byte[] pdf = Base64.getMimeDecoder().decode(base64.replaceAll("\\s+", ""));
					if (pdf.length >= 4 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F') {
						return pdf;
					}
					primerError = primerError == null
							? new IllegalStateException("La etiqueta devuelta por OCA no tiene formato PDF.")
							: primerError;
				} catch (RuntimeException ex) {
					if (primerError == null) {
						primerError = ex;
					}
				}
			}
		}
		throw new IllegalStateException("OCA no devolvio el PDF de la etiqueta.", primerError);
	}

	@Override
	@Transactional
	public String obtenerEtiquetaHtml(Pedido pedido) {
		Envio envio = asegurarEnvioCreado(pedido);
		RuntimeException primerError = null;

		for (String operacion : List.of(
				"GetHtmlDeEtiquetasPorOrdenOrNumeroEnvio",
				"GetHtmlDeEtiquetasPorOrdenOrNumeroEnvioParaEtiquetadora")) {
			for (TipoBusquedaEtiqueta tipoBusqueda : tiposBusquedaEtiqueta(envio)) {
				try {
					String responseXml = solicitarEtiqueta(envio, operacion, tipoBusqueda);
					String html = textOfFirst(parseXml(responseXml), "string", operacion + "Result");
					if (StringUtils.hasText(html)) {
						return html;
					}
				} catch (RuntimeException ex) {
					if (primerError == null) {
						primerError = ex;
					}
				}
			}
		}
		throw new IllegalStateException("OCA no devolvio el HTML de la etiqueta.", primerError);
	}

	@Override
	@Transactional
	public OcaSincronizacionEnvioResponseDTO sincronizarEstadoEnvio(Long envioId) {
		Envio envio = envioRepository.findDetalleById(envioId)
				.orElseThrow(() -> new IllegalArgumentException("Envio no encontrado: " + envioId));
		if (!StringUtils.hasText(envio.getNumeroOrdenRetiro())
				&& !StringUtils.hasText(envio.getNumeroEnvio())
				&& !StringUtils.hasText(envio.getTracking())) {
			throw new IllegalStateException("El envio aun no tiene orden, numero de envio ni tracking para consultar OCA.");
		}
		if (!StringUtils.hasText(properties.getApiUrl()) || !StringUtils.hasText(properties.getCuit())) {
			throw new IllegalStateException("Configura oca.api-url y oca.cuit para sincronizar estados.");
		}

		LocalDate fechaReferencia = Optional.ofNullable(envio.getFechaCreacionEnvio())
				.orElseGet(() -> Optional.ofNullable(envio.getFechaCotizacion()).orElse(LocalDateTime.now()))
				.toLocalDate();
		LocalDate fechaDesde = fechaReferencia.minusDays(10);
		LocalDate fechaHasta = LocalDate.now().plusDays(1);

		try {
			String responseXml = RestClient.create()
					.post()
					.uri(endpoint("List_Envios"))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formListadoEnvios(fechaDesde, fechaHasta))
					.retrieve()
					.body(String.class);

			Document response = parseXml(responseXml);
			String estadoOca = buscarEstadoEnvioEnListado(response, envio);
			EstadoEnvio estadoNuevo = mapearEstadoOca(estadoOca)
					.orElseThrow(() -> new IllegalStateException("OCA no devolvio un estado reconocible para este envio."));
			EstadoEnvio estadoAnterior = envio.getEstadoEnvio();
			boolean actualizado = estadoAnterior != estadoNuevo;
			if (actualizado) {
				envio.setEstadoEnvio(estadoNuevo);
				envioRepository.save(envio);
			}
			String mensaje = actualizado
					? "Estado OCA actualizado: " + estadoAnterior + " -> " + estadoNuevo
					: "Estado OCA sin cambios: " + estadoNuevo;
			return new OcaSincronizacionEnvioResponseDTO(actualizado, estadoAnterior, estadoNuevo, mensaje, responseXml);
		} catch (RestClientResponseException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private String solicitarEtiqueta(Envio envio, String operacion, TipoBusquedaEtiqueta tipoBusqueda) {
		return RestClient.create()
				.post()
				.uri(endpoint(operacion))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formEtiqueta(envio, tipoBusqueda))
				.retrieve()
				.body(String.class);
	}

	private OcaCotizacionRequestDTO crearRequestCotizacion(Pedido pedido) {
		return crearRequestCotizacion(pedido, properties.getOperativa());
	}

	private OcaCotizacionRequestDTO crearRequestCotizacion(Pedido pedido, String operativa) {
		DireccionEnvio direccion = Objects.requireNonNull(pedido.getDireccionEnvio(), "El pedido no tiene direccion de envio");
		ResumenPaquete resumen = resumirPaquete(pedido.getItems());
		return new OcaCotizacionRequestDTO(
				properties.getCuit(),
				operativa,
				properties.getCodigoPostalOrigen(),
				direccion.getCodigoPostal(),
				1,
				BigDecimal.ZERO,
				resumen.pesoTotalKg(),
				resumen.volumenTotalM3(),
				resumen.altoMaxCm(),
				resumen.anchoMaxCm(),
				resumen.largoMaxCm());
	}

	private ResumenPaquete resumirPaquete(List<PedidoItem> items) {
		BigDecimal pesoTotal = BigDecimal.ZERO;
		BigDecimal volumenCm3 = BigDecimal.ZERO;
		BigDecimal altoMax = BigDecimal.ZERO;
		BigDecimal anchoMax = BigDecimal.ZERO;
		BigDecimal largoMax = BigDecimal.ZERO;

		for (PedidoItem item : items) {
			Producto producto = item.getProducto();
			BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
			BigDecimal alto = positivoODefault(producto.getAltoCm(), properties.getAltoDefaultCm());
			BigDecimal ancho = positivoODefault(producto.getAnchoCm(), properties.getAnchoDefaultCm());
			BigDecimal largo = positivoODefault(producto.getLargoCm(), properties.getLargoDefaultCm());
			BigDecimal peso = positivoODefault(producto.getPesoKg(), properties.getPesoDefaultKg());

			pesoTotal = pesoTotal.add(peso.multiply(cantidad));
			volumenCm3 = volumenCm3.add(alto.multiply(ancho).multiply(largo).multiply(cantidad));
			altoMax = altoMax.max(alto);
			anchoMax = anchoMax.max(ancho);
			largoMax = largoMax.max(largo);
		}

		if (items == null || items.isEmpty()) {
			pesoTotal = properties.getPesoDefaultKg();
			altoMax = properties.getAltoDefaultCm();
			anchoMax = properties.getAnchoDefaultCm();
			largoMax = properties.getLargoDefaultCm();
			volumenCm3 = altoMax.multiply(anchoMax).multiply(largoMax);
		}

		BigDecimal volumenM3 = volumenCm3.divide(CM3_EN_M3, 6, RoundingMode.HALF_UP);
		return new ResumenPaquete(pesoTotal, volumenM3, altoMax, anchoMax, largoMax);
	}

	private void cargarDatosBase(Envio envio, Pedido pedido, OcaCotizacionRequestDTO request) {
		DireccionEnvio direccion = pedido.getDireccionEnvio();
		envio.setPedido(pedido);
		envio.setProveedor(ProveedorEnvio.OCA);
		envio.setCodigoPostalOrigen(properties.getCodigoPostalOrigen());
		envio.setCiudadOrigen(properties.getLocalidadOrigen());
		envio.setCodigoPostalDestino(direccion == null ? null : direccion.getCodigoPostal());
		envio.setCiudadDestino(direccion == null ? null : direccion.getCiudad());
		envio.setCuit(properties.getCuit());
		envio.setNumeroCuenta(properties.getNumeroCuenta());
		envio.setOperativa(request.operativa());
		envio.setPesoTotalKg(request.pesoTotalKg());
		envio.setVolumenTotalCm3(request.volumenTotalM3().multiply(CM3_EN_M3));
		envio.setAltoMaxCm(request.altoMaxCm());
		envio.setAnchoMaxCm(request.anchoMaxCm());
		envio.setLargoMaxCm(request.largoMaxCm());
	}

	private String construirXmlEnvio(Pedido pedido, OcaCotizacionRequestDTO request, Envio envio) {
		Cliente cliente = pedido.getCliente();
		DireccionEnvio direccion = pedido.getDireccionEnvio();
		String codigoCompra = valorConDefault(pedido.getCodigoCompra(), "PEDIDO-" + pedido.getId());
		String apellido = valorConDefault(cliente.getApellidoRazonSocial(), cliente.getNombre());
		String nombre = valorConDefault(cliente.getNombre(), "Cliente");
		String telefono = soloDigitos(valorConDefault(cliente.getTelefono(), ""));
		String email = valorConDefault(cliente.getEmail(), "cliente@electrodentalnea.com");
		String pisoDepto = valorConDefault(direccion.getPisoDepto(), "");
		String idCentroImposicionDestino = TIPO_SUCURSAL.equals(envio.getTipoEntregaOca())
				? valorConDefault(envio.getIdCentroImposicionDestino(), "0")
				: "0";

		return "<?xml version=\"1.0\" encoding=\"iso-8859-1\" standalone=\"yes\"?>"
				+ "<ROWS>"
				+ "<cabecera ver=\"2.0\" nrocuenta=\"" + attr(properties.getNumeroCuenta(), 10) + "\" origen=\"API\" />"
				+ "<origenes>"
				+ "<origen"
				+ " calle=\"" + attr(properties.getCalleOrigen(), 30) + "\""
				+ " nro=\"" + attr(properties.getNumeroOrigen(), 5) + "\""
				+ " piso=\"\""
				+ " depto=\"\""
				+ " cp=\"" + attr(properties.getCodigoPostalOrigen(), 4) + "\""
				+ " localidad=\"" + attr(properties.getLocalidadOrigen(), 30) + "\""
				+ " provincia=\"" + attr(properties.getProvinciaOrigen(), 30) + "\""
				+ " contacto=\"" + attr(properties.getContactoOrigen(), 30) + "\""
				+ " email=\"" + attr(properties.getEmailOrigen(), 100) + "\""
				+ " solicitante=\"" + attr(properties.getContactoOrigen(), 30) + "\""
				+ " observaciones=\"" + attr("Pedido " + codigoCompra, 100) + "\""
				+ " centrocosto=\"" + attr(properties.getCentroCosto(), 10) + "\""
				+ " idfranjahoraria=\"" + attr(properties.getIdFranjaHoraria(), 1) + "\""
				+ " idcentroimposicionorigen=\"" + attr(properties.getIdCentroImposicionOrigen(), 3) + "\""
				+ " fecha=\"" + LocalDate.now().format(FECHA_OCA) + "\">"
				+ "<envios>"
				+ "<envio idoperativa=\"" + attr(request.operativa(), 6) + "\" nroremito=\"" + attr(codigoCompra, 30) + "\">"
				+ "<destinatario"
				+ " apellido=\"" + attr(apellido, 30) + "\""
				+ " nombre=\"" + attr(nombre, 30) + "\""
				+ " calle=\"" + attr(direccion.getCalle(), 30) + "\""
				+ " nro=\"" + attr(direccion.getNumero(), 5) + "\""
				+ " piso=\"" + attr(pisoDepto, 6) + "\""
				+ " depto=\"\""
				+ " localidad=\"" + attr(direccion.getCiudad(), 30) + "\""
				+ " provincia=\"" + attr(direccion.getProvincia(), 30) + "\""
				+ " cp=\"" + attr(direccion.getCodigoPostal(), 4) + "\""
				+ " telefono=\"" + attr(telefono, 30) + "\""
				+ " email=\"" + attr(email, 100) + "\""
				+ " idci=\"" + attr(idCentroImposicionDestino, 10) + "\""
				+ " celular=\"" + attr(telefono, 15) + "\""
				+ " observaciones=\"" + attr("Compra web " + codigoCompra, 100) + "\" />"
				+ "<paquetes>"
				+ "<paquete"
				+ " alto=\"" + decimal(request.altoMaxCm(), 2) + "\""
				+ " ancho=\"" + decimal(request.anchoMaxCm(), 2) + "\""
				+ " largo=\"" + decimal(request.largoMaxCm(), 2) + "\""
				+ " peso=\"" + decimal(request.pesoTotalKg(), 3) + "\""
				+ " valor=\"" + decimal(request.valorDeclarado(), 2) + "\""
				+ " cant=\"1\" />"
				+ "</paquetes>"
				+ "</envio>"
				+ "</envios>"
				+ "</origen>"
				+ "</origenes>"
				+ "</ROWS>";
	}

	private MultiValueMap<String, String> formCotizacion(OcaCotizacionRequestDTO request) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("PesoTotal", decimal(request.pesoTotalKg(), 3));
		form.add("VolumenTotal", decimal(request.volumenTotalM3(), 6));
		form.add("CodigoPostalOrigen", request.codigoPostalOrigen());
		form.add("CodigoPostalDestino", request.codigoPostalDestino());
		form.add("CantidadPaquetes", request.cantidadPaquetes().toString());
		form.add("ValorDeclarado", decimal(request.valorDeclarado(), 2));
		form.add("Cuit", request.cuit());
		form.add("Operativa", request.operativa());
		return form;
	}

	private String formCotizacionComoTexto(OcaCotizacionRequestDTO request) {
		return formCotizacion(request).toString();
	}

	private MultiValueMap<String, String> formCreacion(String requestXml) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("usr", properties.getUsuario());
		form.add("psw", properties.getPassword());
		form.add("xml_Datos", requestXml);
		form.add("ConfirmarRetiro", properties.isConfirmarRetiro() ? "True" : "False");
		form.add("ArchivoCliente", "");
		form.add("ArchivoProceso", "");
		return form;
	}

	private MultiValueMap<String, String> formEtiqueta(Envio envio, TipoBusquedaEtiqueta tipoBusqueda) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		String orden = (tipoBusqueda == TipoBusquedaEtiqueta.ORDEN || tipoBusqueda == TipoBusquedaEtiqueta.AMBOS)
				? valorConDefault(envio.getNumeroOrdenRetiro(), "")
				: "";
		String numeroEnvio = (tipoBusqueda == TipoBusquedaEtiqueta.ENVIO || tipoBusqueda == TipoBusquedaEtiqueta.AMBOS)
				? valorConDefault(envio.getNumeroEnvio(), "")
				: "";
		form.add("idOrdenRetiro", orden);
		form.add("ordenRetiro", orden);
		form.add("nroEnvio", numeroEnvio);
		form.add("numeroEnvio", numeroEnvio);
		form.add("logisticaInversa", Boolean.toString(properties.isLogisticaInversa()));
		return form;
	}

	private Envio asegurarEnvioCreado(Pedido pedido) {
		Envio envio = obtenerEnvio(pedido);
		if (!StringUtils.hasText(envio.getNumeroOrdenRetiro()) && !StringUtils.hasText(envio.getNumeroEnvio())) {
			OcaCreacionEnvioResponseDTO creacion = crearEnvio(pedido);
			if (!creacion.creado()) {
				throw new IllegalStateException("No se pudo crear el envio OCA: " + creacion.mensaje());
			}
			envio = obtenerEnvio(pedido);
		}
		return envio;
	}

	private List<TipoBusquedaEtiqueta> tiposBusquedaEtiqueta(Envio envio) {
		List<TipoBusquedaEtiqueta> tipos = new ArrayList<>();
		if (StringUtils.hasText(envio.getNumeroEnvio())) {
			tipos.add(TipoBusquedaEtiqueta.ENVIO);
		}
		if (StringUtils.hasText(envio.getNumeroOrdenRetiro())) {
			tipos.add(TipoBusquedaEtiqueta.ORDEN);
		}
		if (StringUtils.hasText(envio.getNumeroEnvio()) && StringUtils.hasText(envio.getNumeroOrdenRetiro())) {
			tipos.add(TipoBusquedaEtiqueta.AMBOS);
		}
		return tipos;
	}

	private MultiValueMap<String, String> formListadoEnvios(LocalDate fechaDesde, LocalDate fechaHasta) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("CUIT", properties.getCuit());
		form.add("FechaDesde", fechaDesde.format(FECHA_LISTADO_OCA));
		form.add("FechaHasta", fechaHasta.format(FECHA_LISTADO_OCA));
		return form;
	}

	private Envio obtenerEnvio(Pedido pedido) {
		if (pedido.getId() != null) {
			return envioRepository.findByPedidoId(pedido.getId()).orElseGet(Envio::new);
		}
		return pedido.getEnvio() == null ? new Envio() : pedido.getEnvio();
	}

	private String resolverOperativa(String tipoEntregaOca) {
		boolean destinoSucursal = TIPO_SUCURSAL.equals(tipoEntregaOca);
		boolean origenSucursal = origenDesdeSucursal();
		if (origenSucursal) {
			return destinoSucursal
					? properties.getOperativaOrigenSucursalSucursal()
					: properties.getOperativaOrigenSucursalDomicilio();
		}
		return destinoSucursal ? properties.getOperativaSucursal() : properties.getOperativa();
	}

	private boolean origenDesdeSucursal() {
		String idOrigen = properties.getIdCentroImposicionOrigen();
		return StringUtils.hasText(idOrigen) && !"0".equals(idOrigen.trim());
	}

	private boolean configuracionCotizacionCompleta() {
		return configuracionCotizacionCompleta(properties.getOperativa());
	}

	private boolean configuracionCotizacionCompleta(String operativa) {
		return StringUtils.hasText(properties.getApiUrl())
				&& StringUtils.hasText(properties.getCuit())
				&& StringUtils.hasText(operativa)
				&& StringUtils.hasText(properties.getCodigoPostalOrigen());
	}

	private boolean configuracionCreacionCompleta(String operativa) {
		return configuracionCotizacionCompleta(operativa)
				&& StringUtils.hasText(properties.getUsuario())
				&& StringUtils.hasText(properties.getPassword())
				&& StringUtils.hasText(properties.getNumeroCuenta());
	}

	private String endpoint(String operacion) {
		String apiUrl = properties.getApiUrl();
		return (apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl) + "/" + operacion;
	}

	private List<OcaSucursalDTO> parseSucursales(Document document) {
		NodeList centros = document.getElementsByTagName("Centro");
		List<OcaSucursalDTO> sucursales = new ArrayList<>();
		Set<String> vistas = new java.util.LinkedHashSet<>();
		for (int i = 0; i < centros.getLength(); i++) {
			Element centro = (Element) centros.item(i);
			boolean admite = tieneServicio(centro, "admis");
			boolean entrega = tieneServicio(centro, "entrega");
			OcaSucursalDTO sucursal = new OcaSucursalDTO(
					textOfChild(centro, "IdCentroImposicion"),
					textOfChild(centro, "Sigla"),
					textOfChild(centro, "Sucursal"),
					textOfChild(centro, "Calle"),
					textOfChild(centro, "Numero"),
					textOfChild(centro, "Localidad"),
					textOfChild(centro, "CodigoPostal"),
					textOfChild(centro, "Provincia"),
					textOfChild(centro, "Telefono"),
					textOfChild(centro, "TipoAgencia"),
					textOfChild(centro, "HorarioAtencion"),
					textOfChild(centro, "Latitud"),
					textOfChild(centro, "Longitud"),
					admite,
					entrega);
			String claveVista = normalizarOca(sucursal.nombre() + "|" + sucursal.direccionResumen()).toLowerCase(Locale.ROOT);
			if (vistas.add(claveVista)) {
				sucursales.add(sucursal);
			}
		}
		return sucursales;
	}

	private boolean tieneServicio(Element centro, String servicioBuscado) {
		NodeList servicios = centro.getElementsByTagName("ServicioDesc");
		for (int i = 0; i < servicios.getLength(); i++) {
			String servicio = normalizarOca(servicios.item(i).getTextContent()).toLowerCase(Locale.ROOT);
			if (servicio.contains(servicioBuscado)) {
				return true;
			}
		}
		return false;
	}

	private String textOfChild(Element parent, String nombre) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element element && nombre.equals(element.getTagName())) {
				String text = element.getTextContent();
				return StringUtils.hasText(text) ? text.trim() : "";
			}
		}
		return "";
	}

	private String buscarEstadoEnvioEnListado(Document document, Envio envio) {
		Set<String> claves = clavesEnvio(envio);
		Element candidato = null;
		int textoMasCorto = Integer.MAX_VALUE;
		NodeList nodes = document.getElementsByTagName("*");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			String texto = normalizarClave(element.getTextContent());
			if (!StringUtils.hasText(texto) || claves.stream().noneMatch(texto::contains)) {
				continue;
			}
			if (texto.length() < textoMasCorto) {
				candidato = element;
				textoMasCorto = texto.length();
			}
		}
		if (candidato == null) {
			throw new IllegalStateException("No se encontro el envio en el listado devuelto por OCA.");
		}
		String estado = textOfFirst(candidato,
				"Estado",
				"EstadoEnvio",
				"EstadoEntrega",
				"DescripcionEstado",
				"DescEstado",
				"UltimoEstado",
				"Evento",
				"Descripcion",
				"Resultado");
		return StringUtils.hasText(estado) ? estado : candidato.getTextContent();
	}

	private Set<String> clavesEnvio(Envio envio) {
		Set<String> claves = new LinkedHashSet<>();
		agregarClave(claves, envio.getNumeroEnvio());
		agregarClave(claves, envio.getNumeroOrdenRetiro());
		agregarClave(claves, envio.getTracking());
		if (envio.getPedido() != null) {
			agregarClave(claves, envio.getPedido().getCodigoCompra());
			if (envio.getPedido().getId() != null) {
				agregarClave(claves, envio.getPedido().getId().toString());
			}
		}
		return claves;
	}

	private void agregarClave(Set<String> claves, String valor) {
		String clave = normalizarClave(valor);
		if (StringUtils.hasText(clave) && clave.length() >= 3) {
			claves.add(clave);
		}
	}

	private Optional<EstadoEnvio> mapearEstadoOca(String estadoOca) {
		String estado = normalizarClave(estadoOca);
		if (!StringUtils.hasText(estado)) {
			return Optional.empty();
		}
		if (estado.contains("entreg") || estado.contains("recibid")) {
			return Optional.of(EstadoEnvio.ENTREGADO);
		}
		if (estado.contains("despach")
				|| estado.contains("transito")
				|| estado.contains("distribucion")
				|| estado.contains("viaje")
				|| estado.contains("admitid")
				|| estado.contains("sucursal destino")) {
			return Optional.of(EstadoEnvio.DESPACHADO);
		}
		if (estado.contains("pendiente")
				|| estado.contains("generad")
				|| estado.contains("ingresad")
				|| estado.contains("orden")
				|| estado.contains("retiro")) {
			return Optional.of(EstadoEnvio.PENDIENTE_DESPACHO);
		}
		if (estado.contains("cancel")
				|| estado.contains("rechaz")
				|| estado.contains("error")
				|| estado.contains("fall")
				|| estado.contains("devuelt")) {
			return Optional.of(EstadoEnvio.FALLIDO);
		}
		return Optional.empty();
	}

	private Document parseXml(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(new InputSource(new StringReader(valorConDefault(xml, "<xml/>"))));
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo leer la respuesta XML de OCA", ex);
		}
	}

	private BigDecimal decimalDesdeXml(Document document, String... nombres) {
		String valor = textOfFirst(document, nombres);
		if (!StringUtils.hasText(valor)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(valor.trim().replace(",", "."));
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private String textOfFirst(Document document, String... nombres) {
		return textOfFirst(document.getDocumentElement(), nombres);
	}

	private String textOfFirst(Element root, String... nombres) {
		Set<String> buscados = Set.of(nombres).stream()
				.map(nombre -> nombre.toLowerCase(Locale.ROOT))
				.collect(java.util.stream.Collectors.toSet());
		String nombreRaiz = root.getLocalName() == null ? root.getNodeName() : root.getLocalName();
		if (buscados.contains(nombreRaiz.toLowerCase(Locale.ROOT))) {
			String text = root.getTextContent();
			if (StringUtils.hasText(text)) {
				return text.trim();
			}
		}
		NodeList nodes = root.getElementsByTagName("*");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			String nombre = element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
			if (buscados.contains(nombre.toLowerCase(Locale.ROOT))) {
				String text = element.getTextContent();
				if (StringUtils.hasText(text)) {
					return text.trim();
				}
			}
		}
		return null;
	}

	private BigDecimal positivoODefault(BigDecimal valor, BigDecimal defaultValue) {
		return valor != null && valor.compareTo(BigDecimal.ZERO) > 0 ? valor : defaultValue;
	}

	private String decimal(BigDecimal valor, int escala) {
		return (valor == null ? BigDecimal.ZERO : valor)
				.setScale(escala, RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}

	private String attr(String valor, int maxLength) {
		String normalizado = normalizarOca(valor);
		if (normalizado.length() > maxLength) {
			normalizado = normalizado.substring(0, maxLength);
		}
		return normalizado
				.replace("&", "&amp;")
				.replace("\"", "&quot;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private String normalizarOca(String valor) {
		String texto = valorConDefault(valor, "");
		texto = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return texto.replace("|", " ")
				.replace("&", " ")
				.replace("<", " ")
				.replace(">", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private String normalizarClave(String valor) {
		return normalizarOca(valor)
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private String soloDigitos(String valor) {
		return valor == null ? "" : valor.replaceAll("[^0-9]", "");
	}

	private String valorConDefault(String valor, String defaultValue) {
		return valor == null || valor.isBlank() ? defaultValue : valor.trim();
	}

	private record ResumenPaquete(
			BigDecimal pesoTotalKg,
			BigDecimal volumenTotalM3,
			BigDecimal altoMaxCm,
			BigDecimal anchoMaxCm,
			BigDecimal largoMaxCm) {
	}
}
