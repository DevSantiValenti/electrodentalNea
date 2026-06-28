package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.analistas.electrodental.model.domain.Cliente;
import com.analistas.electrodental.model.domain.MetodoPagoVenta;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.VentaPresencial;
import com.analistas.electrodental.model.domain.VentaPresencialItem;
import com.analistas.electrodental.model.domain.dto.VentaPresencialItemRequestDTO;
import com.analistas.electrodental.model.domain.dto.VentaPresencialRequestDTO;
import com.analistas.electrodental.model.repository.IClienteRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.IVentaPresencialRepository;

@ExtendWith(MockitoExtension.class)
class VentaPresencialServiceImplTest {

	@Mock
	IVentaPresencialRepository ventaPresencialRepository;
	@Mock
	IProductoRepository productoRepository;
	@Mock
	IClienteRepository clienteRepository;
	@Mock
	IStockService stockService;

	VentaPresencialServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new VentaPresencialServiceImpl(
				ventaPresencialRepository,
				productoRepository,
				clienteRepository,
				stockService);
	}

	@Test
	void actualizarVentaRestauraStockYConservaPrecioSnapshot() {
		Producto producto = producto(11L, "Turbina", "1500.00", 8);
		VentaPresencial venta = ventaConItem(7L, producto, 2, "999.00");
		Cliente cliente = new Cliente();
		cliente.setDniCuit("30111222");

		when(ventaPresencialRepository.findDetalleById(7L)).thenReturn(Optional.of(venta));
		when(productoRepository.findById(11L)).thenReturn(Optional.of(producto));
		when(clienteRepository.findByDniCuitNormalizado("30111222")).thenReturn(Optional.of(cliente));
		when(clienteRepository.save(cliente)).thenReturn(cliente);
		when(ventaPresencialRepository.save(org.mockito.ArgumentMatchers.any(VentaPresencial.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		VentaPresencial actualizada = service.actualizarVenta(7L, new VentaPresencialRequestDTO(
				List.of(new VentaPresencialItemRequestDTO(11L, 3)),
				"30.111.222",
				"Ana",
				"Cliente",
				"ana@example.com",
				"123",
				MetodoPagoVenta.TARJETA,
				"Admin",
				"Editada"));

		verify(stockService).ajustarStock(producto, 0, 10, "EDICION_VENTA_FISICA #7");
		verify(stockService).registrarVentaFisica(producto, 3, "EDICION_VENTA_FISICA #7");
		assertThat(actualizada.getItems()).hasSize(1);
		assertThat(actualizada.getItems().get(0).getCantidad()).isEqualTo(3);
		assertThat(actualizada.getItems().get(0).getPrecioUnitarioSnapshot()).isEqualByComparingTo("999.00");
		assertThat(actualizada.getTotal()).isEqualByComparingTo("2997.00");
	}

	@Test
	void eliminarVentaDevuelveStockFisicoYBorraLaVenta() {
		Producto producto = producto(11L, "Turbina", "1500.00", 8);
		VentaPresencial venta = ventaConItem(7L, producto, 2, "999.00");

		when(ventaPresencialRepository.findDetalleById(7L)).thenReturn(Optional.of(venta));

		service.eliminarVenta(7L);

		verify(stockService).ajustarStock(producto, 0, 10, "ELIMINACION_VENTA_FISICA #7");
		verify(ventaPresencialRepository).delete(venta);
	}

	private VentaPresencial ventaConItem(Long id, Producto producto, int cantidad, String precio) {
		VentaPresencial venta = new VentaPresencial();
		venta.setId(id);
		VentaPresencialItem item = new VentaPresencialItem();
		item.setProducto(producto);
		item.setNombreSnapshot(producto.getNombre());
		item.setCantidad(cantidad);
		item.setPrecioUnitarioSnapshot(new BigDecimal(precio));
		item.calcularSubtotal();
		venta.agregarItem(item);
		return venta;
	}

	private Producto producto(Long id, String nombre, String precio, int stockFisico) {
		Producto producto = new Producto();
		producto.setId(id);
		producto.setNombre(nombre);
		producto.setPrecio(new BigDecimal(precio));
		producto.setStockWeb(0);
		producto.setStockFisico(stockFisico);
		return producto;
	}
}
