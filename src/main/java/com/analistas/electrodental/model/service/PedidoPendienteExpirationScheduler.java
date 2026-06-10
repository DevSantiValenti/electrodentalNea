package com.analistas.electrodental.model.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.repository.IPedidoRepository;

@Service
public class PedidoPendienteExpirationScheduler {

	private static final long MINUTOS_EXPIRACION = 60;

	private final IPedidoRepository pedidoRepository;
	private final IPedidoService pedidoService;

	public PedidoPendienteExpirationScheduler(IPedidoRepository pedidoRepository, IPedidoService pedidoService) {
		this.pedidoRepository = pedidoRepository;
		this.pedidoService = pedidoService;
	}

	@Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
	public void cancelarPedidosPendientesVencidos() {
		LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_EXPIRACION);
		pedidoRepository.findPendientesVencidosConDetalle(EstadoPedido.PENDIENTE_PAGO, limite)
				.forEach(pedido -> {
					try {
						pedidoService.cancelarPedido(pedido.getId(), "Pedido pendiente expirado");
					} catch (RuntimeException ignored) {
					}
				});
	}
}
