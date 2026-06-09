package com.analistas.electrodental.web.controller;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.repository.IPedidoRepository;
import com.analistas.electrodental.model.repository.IClienteRepository;
import com.analistas.electrodental.model.repository.IVentaPresencialRepository;
import com.analistas.electrodental.model.service.IAdminDashboardService;
import com.analistas.electrodental.model.service.ICategoriaService;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IProductoService;

@Controller
public class AdminController {

	private final IProductoService productoService;
	private final IAdminDashboardService adminDashboardService;
	private final IPedidoRepository pedidoRepository;
	private final IClienteRepository clienteRepository;
	private final IVentaPresencialRepository ventaPresencialRepository;
	private final ICategoriaService categoriaService;
	private final IConfiguracionTiendaService configuracionTiendaService;

	public AdminController(
			IProductoService productoService,
			IAdminDashboardService adminDashboardService,
			IPedidoRepository pedidoRepository,
			IClienteRepository clienteRepository,
			IVentaPresencialRepository ventaPresencialRepository,
			ICategoriaService categoriaService,
			IConfiguracionTiendaService configuracionTiendaService) {
		this.productoService = productoService;
		this.adminDashboardService = adminDashboardService;
		this.pedidoRepository = pedidoRepository;
		this.clienteRepository = clienteRepository;
		this.ventaPresencialRepository = ventaPresencialRepository;
		this.categoriaService = categoriaService;
		this.configuracionTiendaService = configuracionTiendaService;
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
		model.addAttribute("productos", productoService.listarActivos());
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
			@RequestParam(required = false) List<String> caracteristicaNombres,
			@RequestParam(required = false) List<String> caracteristicaDetalles,
			RedirectAttributes redirectAttributes) {
		prepararProducto(producto, categoriaId, subcategoriaId, imagenesProducto, caracteristicaNombres, caracteristicaDetalles);
		productoService.guardar(producto);
		redirectAttributes.addFlashAttribute("mensaje", "Producto guardado correctamente");
		return "redirect:/admin/productos";
	}

	@GetMapping("/admin/productos/{id}/editar")
	public String editarProducto(@PathVariable Long id, Model model) {
		Producto producto = productoService.buscarPorId(id)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
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
			@RequestParam(required = false) List<String> caracteristicaNombres,
			@RequestParam(required = false) List<String> caracteristicaDetalles,
			RedirectAttributes redirectAttributes) {
		producto.setId(id);
		prepararProducto(producto, categoriaId, subcategoriaId, imagenesProducto, caracteristicaNombres, caracteristicaDetalles);
		productoService.guardar(producto);
		redirectAttributes.addFlashAttribute("mensaje", "Producto actualizado correctamente");
		return "redirect:/admin/productos";
	}

	@GetMapping("/admin/pedidos")
	public String pedidos(Model model) {
		model.addAttribute("pedidos", pedidoRepository.findTop10ByOrderByFechaCreacionDesc());
		return "admin/pedidos";
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
			List<String> caracteristicaNombres,
			List<String> caracteristicaDetalles) {
		if (producto.getSlug() == null || producto.getSlug().isBlank()) {
			producto.setSlug(generarSlug(producto.getNombre()));
		}
		producto.setImagenesAdicionales(formatearLineas(imagenesProducto, 10));
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
		producto.setStockWeb(producto.getStockWeb() == null ? 0 : producto.getStockWeb());
		producto.setStockFisico(producto.getStockFisico() == null ? 0 : producto.getStockFisico());
		producto.setStockMinimo(producto.getStockMinimo() == null ? 3 : producto.getStockMinimo());
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

	private String normalizarValor(String valor) {
		return valor == null ? "" : valor.replace("|", " ").trim();
	}

	public record CaracteristicaFormView(String nombre, String detalle) {
	}
}
