package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.CarritoDTO;

class CarritoServiceImplTest {

	private final CarritoServiceImpl service = new CarritoServiceImpl();

	@Test
	void noAgregaProductosDisponiblesSoloParaConsulta() {
		Producto producto = producto();
		producto.setCompraHabilitada(false);

		CarritoDTO carrito = service.agregarProducto(service.nuevoCarrito(), producto, 1);

		assertThat(carrito.items()).isEmpty();
		assertThat(carrito.cantidadTotal()).isZero();
	}

	@Test
	void mantieneComprablesLosProductosExistentesSinValorMigrado() {
		Producto producto = producto();
		producto.setCompraHabilitada(null);

		CarritoDTO carrito = service.agregarProducto(service.nuevoCarrito(), producto, 1);

		assertThat(carrito.items()).hasSize(1);
		assertThat(carrito.cantidadTotal()).isEqualTo(1);
	}

	@Test
	void noAgregaProductosSinStockWeb() {
		Producto producto = producto();
		producto.setStockWeb(0);

		CarritoDTO carrito = service.agregarProducto(service.nuevoCarrito(), producto, 1);

		assertThat(carrito.items()).isEmpty();
		assertThat(carrito.cantidadTotal()).isZero();
	}

	@Test
	void copiaBloqueoDeEnvioOcaAlItemDelCarrito() {
		Producto producto = producto();
		producto.setEnvioOcaDesactivado(true);

		CarritoDTO carrito = service.agregarProducto(service.nuevoCarrito(), producto, 1);

		assertThat(carrito.items()).hasSize(1);
		assertThat(carrito.items().get(0).envioOcaDesactivado()).isTrue();
	}

	@Test
	void usaPrecioDeOfertaParaElItemDelCarrito() {
		Producto producto = producto();
		producto.setOferta(true);
		producto.setPorcentajeOferta(new BigDecimal("20"));

		CarritoDTO carrito = service.agregarProducto(service.nuevoCarrito(), producto, 2);

		assertThat(carrito.items()).hasSize(1);
		assertThat(carrito.items().get(0).tieneOferta()).isTrue();
		assertThat(carrito.items().get(0).precioUnitario()).isEqualByComparingTo("800.00");
		assertThat(carrito.subtotal()).isEqualByComparingTo("1600.00");
	}

	private Producto producto() {
		Producto producto = new Producto();
		producto.setId(1L);
		producto.setNombre("Producto test");
		producto.setSlug("producto-test");
		producto.setPrecio(new BigDecimal("1000"));
		producto.setStockWeb(5);
		return producto;
	}
}
