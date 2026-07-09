package com.analistas.electrodental.model.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.DescuentoCodigo;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.domain.TipoAplicacionDescuento;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.dto.CarritoItemDTO;
import com.analistas.electrodental.model.domain.dto.DescuentoAplicadoDTO;
import com.analistas.electrodental.model.domain.dto.ResultadoDescuentoDTO;
import com.analistas.electrodental.model.repository.ICategoriaRepository;
import com.analistas.electrodental.model.repository.IDescuentoCodigoRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.ISubcategoriaRepository;

@Service
public class DescuentoServiceImpl implements IDescuentoService {

	private static final BigDecimal CIEN = new BigDecimal("100");
	private static final String MENSAJE_OFERTAS_EXENTAS = "Los productos en oferta están exentos del descuento.";

	private final IDescuentoCodigoRepository descuentoRepository;
	private final IProductoRepository productoRepository;
	private final ICategoriaRepository categoriaRepository;
	private final ISubcategoriaRepository subcategoriaRepository;

	public DescuentoServiceImpl(
			IDescuentoCodigoRepository descuentoRepository,
			IProductoRepository productoRepository,
			ICategoriaRepository categoriaRepository,
			ISubcategoriaRepository subcategoriaRepository) {
		this.descuentoRepository = descuentoRepository;
		this.productoRepository = productoRepository;
		this.categoriaRepository = categoriaRepository;
		this.subcategoriaRepository = subcategoriaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DescuentoCodigo> listarTodos() {
		return descuentoRepository.findAllByOrderByFechaCreacionDesc();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DescuentoCodigo> buscarPorId(Long id) {
		return descuentoRepository.findDetalleById(id);
	}

	@Override
	public DescuentoCodigo nuevoDescuento() {
		DescuentoCodigo descuento = new DescuentoCodigo();
		descuento.setFechaInicio(LocalDate.now());
		descuento.setActivo(true);
		descuento.setMontoMinimoCompra(BigDecimal.ZERO);
		return descuento;
	}

	@Override
	@Transactional
	public DescuentoCodigo guardar(
			Long id,
			DescuentoCodigo datos,
			List<Long> productoIds,
			List<Long> categoriaIds,
			List<Long> subcategoriaIds) {
		DescuentoCodigo destino = id == null
				? new DescuentoCodigo()
				: descuentoRepository.findDetalleById(id)
						.orElseThrow(() -> new IllegalArgumentException("Descuento no encontrado: " + id));

		String codigo = normalizarCodigo(datos.getCodigo());
		if (!StringUtils.hasText(codigo)) {
			throw new IllegalArgumentException("El código es obligatorio.");
		}
		if (id == null && descuentoRepository.existsByCodigoIgnoreCase(codigo)) {
			throw new IllegalArgumentException("Ya existe un descuento con ese código.");
		}
		if (id != null && descuentoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
			throw new IllegalArgumentException("Ya existe otro descuento con ese código.");
		}
		BigDecimal porcentaje = dinero(datos.getPorcentajeDescuento());
		if (porcentaje.compareTo(BigDecimal.ZERO) <= 0 || porcentaje.compareTo(CIEN) > 0) {
			throw new IllegalArgumentException("El porcentaje debe ser mayor a 0 y menor o igual a 100.");
		}
		if (datos.getFechaInicio() != null && datos.getFechaVencimiento() != null
				&& datos.getFechaInicio().isAfter(datos.getFechaVencimiento())) {
			throw new IllegalArgumentException("La fecha de inicio no puede ser posterior al vencimiento.");
		}

		destino.setCodigo(codigo);
		destino.setPorcentajeDescuento(porcentaje);
		destino.setTipoAplicacion(datos.getTipoAplicacion() == null ? TipoAplicacionDescuento.CARRITO : datos.getTipoAplicacion());
		destino.setFechaInicio(datos.getFechaInicio());
		destino.setFechaVencimiento(datos.getFechaVencimiento());
		destino.setMontoMinimoCompra(dinero(datos.getMontoMinimoCompra()));
		destino.setCantidadMaximaUsos(datos.getCantidadMaximaUsos() == null || datos.getCantidadMaximaUsos() < 1 ? null : datos.getCantidadMaximaUsos());
		destino.setActivo(Boolean.TRUE.equals(datos.getActivo()));
		if (destino.getUsosActuales() == null) {
			destino.setUsosActuales(0);
		}

		cargarDestinos(destino, productoIds, categoriaIds, subcategoriaIds);
		validarDestino(destino);
		return descuentoRepository.save(destino);
	}

	@Override
	@Transactional
	public void eliminar(Long id) {
		descuentoRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public ResultadoDescuentoDTO aplicar(CarritoDTO carrito, String codigo) {
		CarritoDTO carritoBase = carrito == null ? new CarritoDTO() : carrito.sinDescuento();
		if (carritoBase.items().isEmpty()) {
			return new ResultadoDescuentoDTO(false, "Agregá productos al carrito antes de aplicar un descuento.", carritoBase);
		}
		String codigoNormalizado = normalizarCodigo(codigo);
		if (!StringUtils.hasText(codigoNormalizado)) {
			return new ResultadoDescuentoDTO(false, "Ingresá un código de descuento.", carritoBase);
		}
		Optional<DescuentoCodigo> descuentoOptional = descuentoRepository.findByCodigoIgnoreCase(codigoNormalizado);
		if (descuentoOptional.isEmpty()) {
			return new ResultadoDescuentoDTO(false, "El código ingresado no existe.", carritoBase);
		}
		DescuentoCodigo descuento = descuentoOptional.get();
		descuento.normalizar();
		ResultadoValidacion validacion = validar(descuento, carritoBase);
		if (!validacion.valido()) {
			return new ResultadoDescuentoDTO(false, validacion.mensaje(), carritoBase);
		}
		CarritoDTO carritoConItemsDescontados = aplicarDescuentoEnItems(carritoBase, descuento);
		BigDecimal montoDescuento = carritoConItemsDescontados.items().stream()
				.map(CarritoItemDTO::descuentoAplicado)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		DescuentoAplicadoDTO descuentoAplicado = crearDescuentoAplicado(descuento, validacion.subtotalAplicable(), montoDescuento);
		return new ResultadoDescuentoDTO(
				true,
				"Descuento " + descuentoAplicado.codigo() + " aplicado correctamente. " + MENSAJE_OFERTAS_EXENTAS,
				new CarritoDTO(
						carritoConItemsDescontados.items(),
						carritoConItemsDescontados.subtotal(),
						carritoConItemsDescontados.cantidadTotal(),
						descuentoAplicado));
	}

	@Override
	@Transactional(readOnly = true)
	public CarritoDTO recalcular(CarritoDTO carrito) {
		if (carrito == null || carrito.descuento() == null) {
			return carrito;
		}
		return aplicar(carrito.sinDescuento(), carrito.descuento().codigo()).carrito();
	}

	@Override
	@Transactional
	public void registrarUso(String codigo) {
		String codigoNormalizado = normalizarCodigo(codigo);
		DescuentoCodigo descuento = descuentoRepository.findByCodigoForUpdate(codigoNormalizado)
				.orElseThrow(() -> new IllegalArgumentException("Descuento no encontrado: " + codigoNormalizado));
		descuento.normalizar();
		if (!descuento.tieneUsosDisponibles()) {
			throw new IllegalStateException("El código " + descuento.getCodigo() + " ya no tiene usos disponibles.");
		}
		descuento.setUsosActuales(descuento.getUsosActuales() + 1);
		descuentoRepository.save(descuento);
	}

	private void cargarDestinos(
			DescuentoCodigo descuento,
			List<Long> productoIds,
			List<Long> categoriaIds,
			List<Long> subcategoriaIds) {
		Set<Long> productos = ids(productoIds);
		Set<Long> categorias = ids(categoriaIds);
		Set<Long> subcategorias = ids(subcategoriaIds);

		descuento.getProductos().clear();
		descuento.getCategorias().clear();
		descuento.getSubcategorias().clear();

		switch (descuento.getTipoAplicacion()) {
			case PRODUCTO -> descuento.getProductos().addAll(productosSinOferta(productos));
			case CATEGORIA -> descuento.getCategorias().addAll(categoriaRepository.findAllById(categorias));
			case SUBCATEGORIA -> descuento.getSubcategorias().addAll(subcategoriaRepository.findAllById(subcategorias));
			case SELECCION_PERSONALIZADA -> {
				descuento.getProductos().addAll(productosSinOferta(productos));
				descuento.getCategorias().addAll(categoriaRepository.findAllById(categorias));
				descuento.getSubcategorias().addAll(subcategoriaRepository.findAllById(subcategorias));
			}
			case CARRITO -> {
			}
		}
	}

	private List<Producto> productosSinOferta(Set<Long> productoIds) {
		return productoRepository.findAllById(productoIds).stream()
				.filter(producto -> !producto.tieneOferta())
				.toList();
	}

	private void validarDestino(DescuentoCodigo descuento) {
		boolean invalido = switch (descuento.getTipoAplicacion()) {
			case CARRITO -> false;
			case PRODUCTO -> descuento.getProductos().isEmpty();
			case CATEGORIA -> descuento.getCategorias().isEmpty();
			case SUBCATEGORIA -> descuento.getSubcategorias().isEmpty();
			case SELECCION_PERSONALIZADA -> descuento.getProductos().isEmpty()
					&& descuento.getCategorias().isEmpty()
					&& descuento.getSubcategorias().isEmpty();
		};
		if (invalido) {
			throw new IllegalArgumentException("Seleccioná al menos un producto, categoría o subcategoría para ese tipo de descuento.");
		}
	}

	private ResultadoValidacion validar(DescuentoCodigo descuento, CarritoDTO carrito) {
		if (!Boolean.TRUE.equals(descuento.getActivo())) {
			return ResultadoValidacion.invalido("El código existe, pero está inactivo.");
		}
		LocalDate hoy = LocalDate.now();
		if (descuento.getFechaInicio() != null && hoy.isBefore(descuento.getFechaInicio())) {
			return ResultadoValidacion.invalido("El código todavía no está vigente.");
		}
		if (descuento.getFechaVencimiento() != null && hoy.isAfter(descuento.getFechaVencimiento())) {
			return ResultadoValidacion.invalido("El código está vencido.");
		}
		if (!descuento.tieneUsosDisponibles()) {
			return ResultadoValidacion.invalido("El código ya no tiene usos disponibles.");
		}
		BigDecimal subtotal = dinero(carrito.subtotal());
		if (subtotal.compareTo(dinero(descuento.getMontoMinimoCompra())) < 0) {
			return ResultadoValidacion.invalido("El carrito no alcanza el monto mínimo para este descuento.");
		}
		BigDecimal subtotalAplicable = calcularSubtotalAplicable(descuento, carrito);
		if (subtotalAplicable.compareTo(BigDecimal.ZERO) <= 0) {
			return ResultadoValidacion.invalido("El código no aplica a los productos del carrito. " + MENSAJE_OFERTAS_EXENTAS);
		}
		return ResultadoValidacion.valido(subtotalAplicable);
	}

	private BigDecimal calcularSubtotalAplicable(DescuentoCodigo descuento, CarritoDTO carrito) {
		Set<Long> productoIds = carrito.items().stream()
				.map(CarritoItemDTO::productoId)
				.collect(Collectors.toSet());
		Map<Long, Producto> productos = productoRepository.findAllById(productoIds).stream()
				.collect(Collectors.toMap(Producto::getId, Function.identity()));
		return carrito.items().stream()
				.filter(item -> aplicaItem(descuento, productos.get(item.productoId())))
				.map(CarritoItemDTO::subtotal)
				.map(this::dinero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private CarritoDTO aplicarDescuentoEnItems(CarritoDTO carrito, DescuentoCodigo descuento) {
		Set<Long> productoIds = carrito.items().stream()
				.map(CarritoItemDTO::productoId)
				.collect(Collectors.toSet());
		Map<Long, Producto> productos = productoRepository.findAllById(productoIds).stream()
				.collect(Collectors.toMap(Producto::getId, Function.identity()));
		List<CarritoItemDTO> items = carrito.items().stream()
				.map(item -> {
					boolean aplica = aplicaItem(descuento, productos.get(item.productoId()));
					if (!aplica) {
						return item.sinDescuento();
					}
					BigDecimal monto = dinero(item.subtotal()
							.multiply(descuento.getPorcentajeDescuento())
							.divide(CIEN, 2, RoundingMode.HALF_UP));
					return item.conDescuento(monto);
				})
				.toList();
		return new CarritoDTO(items, carrito.subtotal(), carrito.cantidadTotal(), carrito.descuento());
	}

	private boolean aplicaItem(DescuentoCodigo descuento, Producto producto) {
		if (producto == null) {
			return false;
		}
		if (producto.tieneOferta()) {
			return false;
		}
		Set<Long> productoIds = descuento.getProductos().stream()
				.map(Producto::getId)
				.collect(Collectors.toSet());
		Set<Long> categoriaIds = descuento.getCategorias().stream()
				.map(Categoria::getId)
				.collect(Collectors.toSet());
		Set<Long> subcategoriaIds = descuento.getSubcategorias().stream()
				.map(Subcategoria::getId)
				.collect(Collectors.toSet());

		boolean productoIncluido = productoIds.contains(producto.getId());
		boolean categoriaIncluida = producto.getCategoria() != null && categoriaIds.contains(producto.getCategoria().getId());
		boolean subcategoriaIncluida = producto.getSubcategoria() != null && subcategoriaIds.contains(producto.getSubcategoria().getId());

		return switch (descuento.getTipoAplicacion()) {
			case PRODUCTO -> productoIncluido;
			case CATEGORIA -> categoriaIncluida;
			case SUBCATEGORIA -> subcategoriaIncluida;
			case SELECCION_PERSONALIZADA -> productoIncluido || categoriaIncluida || subcategoriaIncluida;
			case CARRITO -> true;
		};
	}

	private DescuentoAplicadoDTO crearDescuentoAplicado(DescuentoCodigo descuento, BigDecimal subtotalAplicable, BigDecimal monto) {
		return new DescuentoAplicadoDTO(
				descuento.getId(),
				descuento.getCodigo(),
				dinero(descuento.getPorcentajeDescuento()),
				descuento.getTipoAplicacion(),
				dinero(subtotalAplicable),
				dinero(monto),
				descripcion(descuento));
	}

	private String descripcion(DescuentoCodigo descuento) {
		return descuento.getPorcentajeDescuento().stripTrailingZeros().toPlainString()
				+ "% - "
				+ descuento.getTipoAplicacion().getEtiqueta();
	}

	private Set<Long> ids(List<Long> ids) {
		return ids == null ? Set.of() : ids.stream()
				.filter(id -> id != null && id > 0)
				.collect(Collectors.toCollection(HashSet::new));
	}

	private String normalizarCodigo(String codigo) {
		return codigo == null ? "" : codigo.trim().toUpperCase();
	}

	private BigDecimal dinero(BigDecimal valor) {
		return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
	}

	private record ResultadoValidacion(boolean valido, String mensaje, BigDecimal subtotalAplicable) {

		static ResultadoValidacion valido(BigDecimal subtotalAplicable) {
			return new ResultadoValidacion(true, "", subtotalAplicable);
		}

		static ResultadoValidacion invalido(String mensaje) {
			return new ResultadoValidacion(false, mensaje, BigDecimal.ZERO);
		}
	}
}
