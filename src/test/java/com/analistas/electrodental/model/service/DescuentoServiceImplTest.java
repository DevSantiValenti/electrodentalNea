package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.DescuentoCodigo;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.TipoAplicacionDescuento;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;
import com.analistas.electrodental.model.domain.dto.CarritoItemDTO;
import com.analistas.electrodental.model.repository.ICategoriaRepository;
import com.analistas.electrodental.model.repository.IDescuentoCodigoRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.ISubcategoriaRepository;

@ExtendWith(MockitoExtension.class)
class DescuentoServiceImplTest {

	@Mock
	IDescuentoCodigoRepository descuentoRepository;
	@Mock
	IProductoRepository productoRepository;
	@Mock
	ICategoriaRepository categoriaRepository;
	@Mock
	ISubcategoriaRepository subcategoriaRepository;

	DescuentoServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new DescuentoServiceImpl(
				descuentoRepository,
				productoRepository,
				categoriaRepository,
				subcategoriaRepository);
	}

	@Test
	void descuentoDeCarritoNoAplicaSobreProductosEnOferta() {
		DescuentoCodigo descuento = descuentoCarrito();
		Producto regular = producto(1L, false);
		Producto oferta = producto(2L, true);
		CarritoDTO carrito = new CarritoDTO(
				List.of(item(1L, "100.00"), item(2L, "200.00")),
				new BigDecimal("300.00"),
				2);
		when(descuentoRepository.findByCodigoIgnoreCase("DENTAL10")).thenReturn(Optional.of(descuento));
		when(productoRepository.findAllById(any())).thenReturn(List.of(regular, oferta));

		var resultado = service.aplicar(carrito, "DENTAL10");

		assertThat(resultado.valido()).isTrue();
		assertThat(resultado.mensaje()).contains("productos en oferta");
		assertThat(resultado.carrito().descuentoTotal()).isEqualByComparingTo("10.00");
		assertThat(resultado.carrito().items().get(0).descuentoAplicado()).isEqualByComparingTo("10.00");
		assertThat(resultado.carrito().items().get(1).descuentoAplicado()).isZero();
	}

	@Test
	void descuentoNoAplicaSiTodosLosProductosEstanEnOferta() {
		DescuentoCodigo descuento = descuentoCarrito();
		Producto oferta = producto(2L, true);
		CarritoDTO carrito = new CarritoDTO(
				List.of(item(2L, "200.00")),
				new BigDecimal("200.00"),
				1);
		when(descuentoRepository.findByCodigoIgnoreCase("DENTAL10")).thenReturn(Optional.of(descuento));
		when(productoRepository.findAllById(any())).thenReturn(List.of(oferta));

		var resultado = service.aplicar(carrito, "DENTAL10");

		assertThat(resultado.valido()).isFalse();
		assertThat(resultado.mensaje()).contains("productos en oferta");
		assertThat(resultado.carrito().tieneDescuento()).isFalse();
		assertThat(resultado.carrito().items().get(0).descuentoAplicado()).isZero();
	}

	@Test
	void descuentoDeCategoriaTambienExcluyeProductosEnOferta() {
		Categoria categoria = new Categoria();
		categoria.setId(8L);
		DescuentoCodigo descuento = descuentoCarrito();
		descuento.setTipoAplicacion(TipoAplicacionDescuento.CATEGORIA);
		descuento.getCategorias().add(categoria);
		Producto regular = producto(1L, false);
		regular.setCategoria(categoria);
		Producto oferta = producto(2L, true);
		oferta.setCategoria(categoria);
		CarritoDTO carrito = new CarritoDTO(
				List.of(item(1L, "100.00"), item(2L, "200.00")),
				new BigDecimal("300.00"),
				2);
		when(descuentoRepository.findByCodigoIgnoreCase("DENTAL10")).thenReturn(Optional.of(descuento));
		when(productoRepository.findAllById(any())).thenReturn(List.of(regular, oferta));

		var resultado = service.aplicar(carrito, "DENTAL10");

		assertThat(resultado.valido()).isTrue();
		assertThat(resultado.carrito().descuentoTotal()).isEqualByComparingTo("10.00");
		assertThat(resultado.carrito().items().get(0).descuentoAplicado()).isEqualByComparingTo("10.00");
		assertThat(resultado.carrito().items().get(1).descuentoAplicado()).isZero();
	}

	private DescuentoCodigo descuentoCarrito() {
		DescuentoCodigo descuento = new DescuentoCodigo();
		descuento.setId(1L);
		descuento.setCodigo("DENTAL10");
		descuento.setPorcentajeDescuento(new BigDecimal("10.00"));
		descuento.setTipoAplicacion(TipoAplicacionDescuento.CARRITO);
		descuento.setActivo(true);
		descuento.setMontoMinimoCompra(BigDecimal.ZERO);
		descuento.setUsosActuales(0);
		return descuento;
	}

	private Producto producto(Long id, boolean oferta) {
		Producto producto = new Producto();
		producto.setId(id);
		producto.setNombre("Producto " + id);
		producto.setOferta(oferta);
		return producto;
	}

	private CarritoItemDTO item(Long productoId, String subtotal) {
		return new CarritoItemDTO(
				productoId,
				"producto-" + productoId,
				"Producto " + productoId,
				"/img/test.png",
				new BigDecimal(subtotal),
				1,
				new BigDecimal(subtotal),
				10,
				false);
	}
}
