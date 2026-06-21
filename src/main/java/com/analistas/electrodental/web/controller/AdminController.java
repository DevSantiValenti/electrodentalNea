package com.analistas.electrodental.web.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.domain.Envio;
import com.analistas.electrodental.model.domain.Pedido;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.repository.IEnvioRepository;
import com.analistas.electrodental.model.repository.IPedidoRepository;
import com.analistas.electrodental.model.repository.IClienteRepository;
import com.analistas.electrodental.model.repository.IVentaPresencialRepository;
import com.analistas.electrodental.model.service.IAdminDashboardService;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IOcaService;
import com.analistas.electrodental.model.service.IPedidoService;
import com.analistas.electrodental.model.service.IProductoService;
import com.analistas.electrodental.model.service.ProductoImagenStorageService;

@Controller
public class AdminController {

	private static final int ENVIOS_PAGE_SIZE = 10;
	private static final int PEDIDOS_PAGE_SIZE = 10;

	private final IProductoService productoService;
	private final IAdminDashboardService adminDashboardService;
	private final IPedidoRepository pedidoRepository;
	private final IEnvioRepository envioRepository;
	private final IClienteRepository clienteRepository;
	private final IVentaPresencialRepository ventaPresencialRepository;
	private final ICategoriaService categoriaService;
	private final IConfiguracionTiendaService configuracionTiendaService;
	private final IOcaService ocaService;
	private final IPedidoService pedidoService;
	private final ProductoImagenStorageService productoImagenStorageService;

	public AdminController(
			IProductoService productoService,
			IAdminDashboardService adminDashboardService,
			IPedidoRepository pedidoRepository,
			IEnvioRepository envioRepository,
			IClienteRepository clienteRepository,
			IVentaPresencialRepository ventaPresencialRepository,
			ICategoriaService categoriaService,
			IConfiguracionTiendaService configuracionTiendaService,
			IOcaService ocaService,
			IPedidoService pedidoService,
			ProductoImagenStorageService productoImagenStorageService) {
		this.productoService = productoService;
		this.adminDashboardService = adminDashboardService;
		this.pedidoRepository = pedidoRepository;
		this.envioRepository = envioRepository;
		this.clienteRepository = clienteRepository;
		this.ventaPresencialRepository = ventaPresencialRepository;
		this.categoriaService = categoriaService;
		this.configuracionTiendaService = configuracionTiendaService;
		this.ocaService = ocaService;
		this.pedidoService = pedidoService;
		this.productoImagenStorageService = productoImagenStorageService;
	}

	@GetMapping("/admin/login")
	public String login(Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			return "redirect:/admin";
		}
		return "admin/login";
	}

	@GetMapping({ "/admin", "/panel" })
	public String dashboard(Model model) {
		model.addAttribute("dashboard", adminDashboardService.obtenerMetricas());
		return "admin/dashboard";
	}

	@GetMapping("/admin/productos")
	public String productos(Model model) {
		model.addAttribute("productos", productoService.listarTodos());
		return "admin/productos";
	}

	@GetMapping("/admin/productos/nuevo")
	public String nuevoProducto(Model model) {
		Producto producto = new Producto();
		model.addAttribute("producto", producto);
		model.addAttribute("categorias", categoriaService.listarActivas());
		cargarCamposEditablesProducto(model, producto);
		return "admin/producto-form";
	}

	@PostMapping("/admin/productos")
	public String guardarProducto(
			Producto producto,
			@RequestParam(required = false) Long categoriaId,
			@RequestParam(required = false) Long subcategoriaId,
			@RequestParam(required = false) List<String> imagenesProducto,
			@RequestParam(name = "imagenPrincipalArchivo", required = false) MultipartFile imagenPrincipalArchivo,
			@RequestParam(name = "imagenesProductoArchivos", required = false) List<MultipartFile> imagenesProductoArchivos,
			@RequestParam(required = false) List<String> caracteristicaNombres,
			@RequestParam(required = false) List<String> caracteristicaDetalles,
			RedirectAttributes redirectAttributes) {
		try {
			prepararProducto(producto, categoriaId, subcategoriaId, imagenesProducto, imagenPrincipalArchivo, imagenesProductoArchivos, caracteristicaNombres, caracteristicaDetalles);
			productoService.guardar(producto);
			redirectAttributes.addFlashAttribute("mensaje", "Producto guardado correctamente");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo guardar el producto: " + ex.getMessage());
			return "redirect:/admin/productos/nuevo";
		}
		return "redirect:/admin/productos";
	}

	@GetMapping("/admin/productos/{id}/editar")
	public String editarProducto(@PathVariable Long id, Model model) {
		Producto producto = productoService.buscarPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
		if (producto.getCompraHabilitada() == null) {
			producto.setCompraHabilitada(true);
		}
		model.addAttribute("producto", producto);
		model.addAttribute("categorias", categoriaService.listarActivas());
		cargarCamposEditablesProducto(model, producto);
		return "admin/producto-form";
	}

	@PostMapping("/admin/productos/{id}")
	public String actualizarProducto(
			@PathVariable Long id,
			Producto producto,
			@RequestParam(required = false) Long categoriaId,
			@RequestParam(required = false) Long subcategoriaId,
			@RequestParam(required = false) List<String> imagenesProducto,
			@RequestParam(name = "imagenPrincipalArchivo", required = false) MultipartFile imagenPrincipalArchivo,
			@RequestParam(name = "imagenesProductoArchivos", required = false) List<MultipartFile> imagenesProductoArchivos,
			@RequestParam(required = false) List<String> caracteristicaNombres,
			@RequestParam(required = false) List<String> caracteristicaDetalles,
			RedirectAttributes redirectAttributes) {
		producto.setId(id);
		try {
			prepararProducto(producto, categoriaId, subcategoriaId, imagenesProducto, imagenPrincipalArchivo, imagenesProductoArchivos, caracteristicaNombres, caracteristicaDetalles);
			productoService.guardar(producto);
			redirectAttributes.addFlashAttribute("mensaje", "Producto actualizado correctamente");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo actualizar el producto: " + ex.getMessage());
			return "redirect:/admin/productos/" + id + "/editar";
		}
		return "redirect:/admin/productos";
	}

	@PostMapping("/admin/productos/{id}/eliminar")
	public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (productoService.buscarPorId(id).isEmpty()) {
			redirectAttributes.addFlashAttribute("mensaje", "El producto ya no existe.");
			return "redirect:/admin/productos";
		}
		try {
			productoService.eliminar(id);
			redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado correctamente.");
		} catch (DataIntegrityViolationException ex) {
			redirectAttributes.addFlashAttribute("mensaje",
					"No se puede eliminar porque el producto tiene pedidos o ventas asociados. Podés marcarlo como inactivo.");
		}
		return "redirect:/admin/productos";
	}

	@GetMapping("/admin/pedidos")
	public String pedidos(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		cargarPedidos(model, q, fechaDesde, fechaHasta, page);
		return "admin/pedidos";
	}

	@GetMapping("/admin/pedidos/buscar")
	public String buscarPedidos(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		cargarPedidos(model, q, fechaDesde, fechaHasta, page);
		return "admin/pedidos :: tablaPedidos";
	}

	@GetMapping("/admin/envios")
	public String envios(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		cargarEnvios(model, q, fechaDesde, fechaHasta, page);
		return "admin/envios";
	}

	@GetMapping("/admin/envios/buscar")
	public String buscarEnvios(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		cargarEnvios(model, q, fechaDesde, fechaHasta, page);
		return "admin/envios :: tablaEnvios";
	}

	@PostMapping("/admin/envios/{id}/sincronizar")
	public String sincronizarEnvio(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			var sincronizacion = ocaService.sincronizarEstadoEnvio(id);
			redirectAttributes.addFlashAttribute("mensaje", sincronizacion.mensaje());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo sincronizar OCA: " + ex.getMessage());
		}
		return "redirect:/admin/envios";
	}

	@GetMapping("/admin/pedidos/{id}")
	public String detallePedido(@PathVariable Long id, Model model) {
		model.addAttribute("pedido", pedidoRepository.findDetalleById(id)
				.orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id)));
		return "admin/pedido-detalle";
	}

	@GetMapping("/admin/pedidos/{id}/imprimir")
	public String imprimirPedido(@PathVariable Long id, Model model) {
		model.addAttribute("pedido", pedidoRepository.findDetalleById(id)
				.orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id)));
		return "admin/pedido-print";
	}

	@GetMapping("/admin/pedidos/{id}/envio/ticket")
	public Object descargarTicketEnvio(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		var pedido = pedidoRepository.findDetalleById(id)
				.orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
		if (!"OCA".equals(pedido.getMetodoEntrega())) {
			throw new IllegalArgumentException("El pedido no tiene envío OCA.");
		}
		String html;
		try {
			html = ocaService.obtenerEtiquetaHtml(pedido);
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo descargar la etiqueta OCA: " + ex.getMessage());
			return "redirect:/admin/pedidos/" + id;
		}
		String filename = "etiqueta-oca-pedido-" + pedido.getId() + ".html";
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_HTML)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
				.body(html);
	}

	@PostMapping("/admin/pedidos/{id}/envio/sincronizar")
	public String sincronizarEnvioPedido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		var pedido = pedidoRepository.findDetalleById(id)
				.orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
		if (pedido.getEnvio() == null) {
			redirectAttributes.addFlashAttribute("mensaje", "El pedido no tiene envío OCA para sincronizar.");
			return "redirect:/admin/pedidos/" + id;
		}
		try {
			var sincronizacion = ocaService.sincronizarEstadoEnvio(pedido.getEnvio().getId());
			redirectAttributes.addFlashAttribute("mensaje", sincronizacion.mensaje());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo sincronizar OCA: " + ex.getMessage());
		}
		return "redirect:/admin/pedidos/" + id;
	}

	@PostMapping("/admin/pedidos/{id}/eliminar")
	public String eliminarPedido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (!pedidoRepository.existsById(id)) {
			redirectAttributes.addFlashAttribute("mensaje", "El pedido ya no existe.");
			return "redirect:/admin/pedidos";
		}
		pedidoRepository.deleteById(id);
		redirectAttributes.addFlashAttribute("mensaje", "Pedido eliminado correctamente.");
		return "redirect:/admin/pedidos";
	}

	@PostMapping("/admin/pedidos/{id}/cancelar-pendiente")
	public String cancelarPedidoPendiente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			pedidoService.cancelarPedido(id, "Cancelado manualmente desde admin");
			redirectAttributes.addFlashAttribute("mensaje", "Pedido cancelado y reserva liberada.");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se pudo cancelar el pedido: " + ex.getMessage());
		}
		return "redirect:/admin/pedidos/" + id;
	}

	@GetMapping("/admin/ventas")
	public String ventas(Model model) {
		model.addAttribute("ventas", ventaPresencialRepository.findTop10ByOrderByFechaDesc());
		model.addAttribute("dashboard", adminDashboardService.obtenerMetricas());
		return "admin/ventas";
	}

	@GetMapping({ "/admin/configuracion", "/admin/configuración" })
	public String configuracion(Model model) {
		model.addAttribute("configuracion", configuracionTiendaService.obtener());
		return "admin/configuracion";
	}

	@PostMapping({ "/admin/configuracion", "/admin/configuración" })
	public String guardarConfiguracion(
			ConfiguracionTienda configuracion,
			@RequestParam(required = false) List<String> emails,
			@RequestParam(required = false) String adminPassword,
			RedirectAttributes redirectAttributes) {
		configuracion.setEmail(emails == null ? null : String.join(",", emails));
		configuracionTiendaService.guardar(configuracion, adminPassword);
		redirectAttributes.addFlashAttribute("mensaje", "Configuración actualizada correctamente");
		return "redirect:/admin/configuracion";
	}

	@GetMapping("/admin/clientes")
	public String clientes(Model model) {
		model.addAttribute("clientes", clienteRepository.findAll());
		return "admin/clientes";
	}

	@GetMapping("/admin/clientes/nuevo")
	public String nuevoCliente(Model model) {
		model.addAttribute("cliente", new Cliente());
		return "admin/cliente-form";
	}

	@PostMapping("/admin/clientes")
	public String guardarCliente(Cliente cliente, RedirectAttributes redirectAttributes) {
		prepararCliente(cliente);
		clienteRepository.save(cliente);
		redirectAttributes.addFlashAttribute("mensaje", "Cliente guardado correctamente");
		return "redirect:/admin/clientes";
	}

	@GetMapping("/admin/clientes/{id}/editar")
	public String editarCliente(@PathVariable Long id, Model model) {
		model.addAttribute("cliente", clienteRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id)));
		return "admin/cliente-form";
	}

	@PostMapping("/admin/clientes/{id}")
	public String actualizarCliente(@PathVariable Long id, Cliente cliente, RedirectAttributes redirectAttributes) {
		Cliente actual = clienteRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
		copiarCliente(cliente, actual);
		prepararCliente(actual);
		clienteRepository.save(actual);
		redirectAttributes.addFlashAttribute("mensaje", "Cliente actualizado correctamente");
		return "redirect:/admin/clientes";
	}

	@PostMapping("/admin/clientes/{id}/eliminar")
	public String eliminarCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (!clienteRepository.existsById(id)) {
			redirectAttributes.addFlashAttribute("mensaje", "El cliente ya no existe.");
			return "redirect:/admin/clientes";
		}
		try {
			clienteRepository.deleteById(id);
			clienteRepository.flush();
			redirectAttributes.addFlashAttribute("mensaje", "Cliente eliminado correctamente.");
		} catch (DataIntegrityViolationException ex) {
			redirectAttributes.addFlashAttribute("mensaje", "No se puede eliminar: el cliente tiene pedidos o ventas asociadas.");
		}
		return "redirect:/admin/clientes";
	}

	private void prepararProducto(
			Producto producto,
			Long categoriaId,
			Long subcategoriaId,
			List<String> imagenesProducto,
			MultipartFile imagenPrincipalArchivo,
			List<MultipartFile> imagenesProductoArchivos,
			List<String> caracteristicaNombres,
			List<String> caracteristicaDetalles) {
		if (producto.getSlug() == null || producto.getSlug().isBlank()) {
			producto.setSlug(generarSlug(producto.getNombre()));
		}
		List<String> imagenesGaleria = prepararImagenesGaleria(imagenesProducto, imagenesProductoArchivos);
		String imagenPrincipal = normalizarValorSimple(producto.getImagenPrincipal());
		if (imagenPrincipalArchivo != null && !imagenPrincipalArchivo.isEmpty()) {
			imagenPrincipal = productoImagenStorageService.guardar(imagenPrincipalArchivo);
		}
		if (imagenPrincipal.isBlank() && !imagenesGaleria.isEmpty()) {
			imagenPrincipal = imagenesGaleria.remove(0);
		}
		producto.setImagenPrincipal(imagenPrincipal);
		producto.setImagenesAdicionales(formatearLineas(imagenesGaleria, 10));
		producto.setCaracteristicas(formatearCaracteristicas(caracteristicaNombres, caracteristicaDetalles));
		if (categoriaId != null) {
			categoriaService.buscarCategoriaPorId(categoriaId).ifPresent(producto::setCategoria);
		}
		if (subcategoriaId != null) {
			categoriaService.buscarSubcategoriaPorId(subcategoriaId).ifPresent(subcategoria -> {
				producto.setSubcategoria(subcategoria);
				producto.setCategoria(subcategoria.getCategoria());
			});
		}
		producto.setActivo(producto.getActivo() != null && producto.getActivo());
		producto.setDestacado(producto.getDestacado() != null && producto.getDestacado());
		producto.setOferta(producto.getOferta() != null && producto.getOferta());
		producto.setCompraHabilitada(producto.getCompraHabilitada() != null && producto.getCompraHabilitada());
		producto.setStockWeb(producto.getStockWeb() == null ? 0 : producto.getStockWeb());
		producto.setStockFisico(producto.getStockFisico() == null ? 0 : producto.getStockFisico());
		producto.setStockMinimo(producto.getStockMinimo() == null ? 3 : producto.getStockMinimo());
	}

	private List<String> prepararImagenesGaleria(List<String> imagenesProducto, List<MultipartFile> imagenesProductoArchivos) {
		List<String> imagenes = new ArrayList<>();
		if (imagenesProducto != null) {
			imagenesProducto.stream()
					.map(this::normalizarValorSimple)
					.filter(valor -> !valor.isBlank())
					.limit(10)
					.forEach(imagenes::add);
		}
		if (imagenesProductoArchivos != null) {
			for (MultipartFile archivo : imagenesProductoArchivos) {
				if (imagenes.size() >= 10) {
					break;
				}
				if (archivo != null && !archivo.isEmpty()) {
					imagenes.add(productoImagenStorageService.guardar(archivo));
				}
			}
		}
		return imagenes;
	}

	private String generarSlug(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			return "producto-" + System.currentTimeMillis();
		}
		return nombre.toLowerCase()
				.replaceAll("[^a-z0-9áéíóúñ]+", "-")
				.replaceAll("^-|-$", "");
	}

	private void cargarCamposEditablesProducto(Model model, Producto producto) {
		model.addAttribute("imagenesProducto", parsearLineas(producto.getImagenesAdicionales()));
		model.addAttribute("caracteristicasProducto", parsearCaracteristicas(producto.getCaracteristicas()));
	}

	private List<String> parsearLineas(String valor) {
		if (valor == null || valor.isBlank()) {
			return List.of("");
		}
		List<String> lineas = valor.lines()
				.map(String::trim)
				.filter(linea -> !linea.isBlank())
				.limit(10)
				.toList();
		return lineas.isEmpty() ? List.of("") : lineas;
	}

	private List<CaracteristicaFormView> parsearCaracteristicas(String valor) {
		if (valor == null || valor.isBlank()) {
			return List.of(new CaracteristicaFormView("", ""));
		}
		List<CaracteristicaFormView> caracteristicas = valor.lines()
				.map(String::trim)
				.filter(linea -> !linea.isBlank())
				.map(linea -> {
					String[] partes = linea.split("\\|", 2);
					if (partes.length == 2) {
						return new CaracteristicaFormView(partes[0].trim(), partes[1].trim());
					}
					return new CaracteristicaFormView(linea, "");
				})
				.toList();
		return caracteristicas.isEmpty() ? List.of(new CaracteristicaFormView("", "")) : caracteristicas;
	}

	private String formatearLineas(List<String> valores, int maximo) {
		if (valores == null) {
			return "";
		}
		return valores.stream()
				.map(valor -> valor == null ? "" : valor.trim())
				.filter(valor -> !valor.isBlank())
				.limit(maximo)
				.reduce((actual, siguiente) -> actual + "\n" + siguiente)
				.orElse("");
	}

	private String formatearCaracteristicas(List<String> nombres, List<String> detalles) {
		if (nombres == null) {
			return "";
		}
		return IntStream.range(0, nombres.size())
				.mapToObj(indice -> {
					String nombre = normalizarValor(nombres.get(indice));
					String detalle = detalles != null && detalles.size() > indice ? normalizarValor(detalles.get(indice)) : "";
					if (nombre.isBlank() && detalle.isBlank()) {
						return "";
					}
					return nombre + "|" + detalle;
				})
				.filter(linea -> !linea.isBlank())
				.reduce((actual, siguiente) -> actual + "\n" + siguiente)
				.orElse("");
	}

	private void copiarCliente(Cliente origen, Cliente destino) {
		destino.setNombre(origen.getNombre());
		destino.setApellidoRazonSocial(origen.getApellidoRazonSocial());
		destino.setEmail(origen.getEmail());
		destino.setTelefono(origen.getTelefono());
		destino.setDniCuit(origen.getDniCuit());
	}

	private void prepararCliente(Cliente cliente) {
		cliente.setNombre(valorConDefault(cliente.getNombre(), "Cliente sin nombre"));
		cliente.setApellidoRazonSocial(normalizarValorSimple(cliente.getApellidoRazonSocial()));
		cliente.setEmail(normalizarValorSimple(cliente.getEmail()));
		cliente.setTelefono(normalizarValorSimple(cliente.getTelefono()));
		cliente.setDniCuit(normalizarDniCuit(cliente.getDniCuit()));
	}

	private String normalizarDniCuit(String valor) {
		return valor == null ? "" : valor.replaceAll("[^0-9]", "").trim();
	}

	private String normalizarValorSimple(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private String valorConDefault(String valor, String defaultValue) {
		return valor == null || valor.isBlank() ? defaultValue : valor.trim();
	}

	private void cargarEnvios(Model model, String q, LocalDate fechaDesde, LocalDate fechaHasta, int page) {
		String termino = StringUtils.hasText(q) ? "%" + q.trim().toLowerCase() + "%" : null;
		LocalDateTime desde = fechaDesde == null ? null : fechaDesde.atStartOfDay();
		LocalDateTime hasta = fechaHasta == null ? null : fechaHasta.plusDays(1).atStartOfDay().minusNanos(1);
		int pagina = Math.max(0, page);
		Page<Envio> enviosPage = envioRepository.buscarDetalle(termino, desde, hasta, PageRequest.of(pagina, ENVIOS_PAGE_SIZE));
		if (enviosPage.getTotalPages() > 0 && pagina >= enviosPage.getTotalPages()) {
			enviosPage = envioRepository.buscarDetalle(termino, desde, hasta, PageRequest.of(enviosPage.getTotalPages() - 1, ENVIOS_PAGE_SIZE));
		}
		model.addAttribute("enviosPage", enviosPage);
		model.addAttribute("envios", enviosPage.getContent());
		model.addAttribute("q", StringUtils.hasText(q) ? q.trim() : "");
		model.addAttribute("fechaDesde", fechaDesde);
		model.addAttribute("fechaHasta", fechaHasta);
	}

	private void cargarPedidos(Model model, String q, LocalDate fechaDesde, LocalDate fechaHasta, int page) {
		String termino = StringUtils.hasText(q) ? "%" + q.trim().toLowerCase() + "%" : null;
		LocalDateTime desde = fechaDesde == null ? null : fechaDesde.atStartOfDay();
		LocalDateTime hasta = fechaHasta == null ? null : fechaHasta.plusDays(1).atStartOfDay().minusNanos(1);
		int pagina = Math.max(0, page);
		Page<Pedido> pedidosPage = pedidoRepository.buscarDetalle(termino, desde, hasta, PageRequest.of(pagina, PEDIDOS_PAGE_SIZE));
		if (pedidosPage.getTotalPages() > 0 && pagina >= pedidosPage.getTotalPages()) {
			pedidosPage = pedidoRepository.buscarDetalle(termino, desde, hasta, PageRequest.of(pedidosPage.getTotalPages() - 1, PEDIDOS_PAGE_SIZE));
		}
		model.addAttribute("pedidosPage", pedidosPage);
		model.addAttribute("pedidos", pedidosPage.getContent());
		model.addAttribute("q", StringUtils.hasText(q) ? q.trim() : "");
		model.addAttribute("fechaDesde", fechaDesde);
		model.addAttribute("fechaHasta", fechaHasta);
	}

	private String normalizarValor(String valor) {
		return valor == null ? "" : valor.replace("|", " ").trim();
	}

	public record CaracteristicaFormView(String nombre, String detalle) {
	}
}
