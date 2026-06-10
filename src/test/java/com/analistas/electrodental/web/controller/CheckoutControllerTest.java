package com.analistas.electrodental.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.analistas.electrodental.model.domain.ConfiguracionTienda;
import com.analistas.electrodental.model.repository.IPagoRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.service.IConfiguracionTiendaService;
import com.analistas.electrodental.model.service.IMercadoPagoService;
import com.analistas.electrodental.model.service.IOcaService;
import com.analistas.electrodental.model.service.IPedidoService;
import com.analistas.electrodental.web.config.MercadoPagoProperties;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

	@Mock
	IPedidoService pedidoService;
	@Mock
	IMercadoPagoService mercadoPagoService;
	@Mock
	IProductoRepository productoRepository;
	@Mock
	IPagoRepository pagoRepository;
	@Mock
	IConfiguracionTiendaService configuracionTiendaService;
	@Mock
	IOcaService ocaService;

	CheckoutController controller;

	@BeforeEach
	void setUp() {
		controller = new CheckoutController(
				pedidoService,
				mercadoPagoService,
				productoRepository,
				pagoRepository,
				new MercadoPagoProperties(),
				configuracionTiendaService,
				ocaService);
		ConfiguracionTienda configuracion = new ConfiguracionTienda();
		when(configuracionTiendaService.obtener()).thenReturn(configuracion);
	}

	@Test
	void retornoSinPaymentIdNoActualizaEstadoDelPedido() {
		Model model = new ExtendedModelMap();
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/checkout/mercadopago/success");

		String view = controller.retornoMercadoPago(null, null, "approved", model, session, request);

		assertThat(view).isEqualTo("checkout-resultado");
		assertThat(model.getAttribute("mensaje")).isEqualTo("Mercado Pago informó estado: approved");
		verify(pedidoService, never()).actualizarPagoMercadoPago(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}
}
