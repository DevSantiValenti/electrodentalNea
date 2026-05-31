package com.analistas.electrodental.model.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.EstadoPedido;
import com.analistas.electrodental.model.domain.dto.AdminDashboardDTO;
import com.analistas.electrodental.model.repository.IPedidoRepository;
import com.analistas.electrodental.model.repository.IProductoRepository;
import com.analistas.electrodental.model.repository.IVentaPresencialRepository;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements IAdminDashboardService {

	private final IPedidoRepository pedidoRepository;
	private final IProductoRepository productoRepository;
	private final IVentaPresencialRepository ventaPresencialRepository;

	public AdminDashboardServiceImpl(
			IPedidoRepository pedidoRepository,
			IProductoRepository productoRepository,
			IVentaPresencialRepository ventaPresencialRepository) {
		this.pedidoRepository = pedidoRepository;
		this.productoRepository = productoRepository;
		this.ventaPresencialRepository = ventaPresencialRepository;
	}

	@Override
	public AdminDashboardDTO obtenerMetricas() {
		LocalDate hoy = LocalDate.now();
		LocalDateTime inicioHoy = hoy.atStartOfDay();
		LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay().minusNanos(1);
		YearMonth mesActual = YearMonth.now();

		long pedidosHoy = pedidoRepository.countByFechaCreacionBetween(inicioHoy, finHoy);
		long pendientes = pedidoRepository.countByEstadoPedido(EstadoPedido.PENDIENTE_PAGO);
		long ventasPresencialesHoy = ventaPresencialRepository.countByFechaBetween(inicioHoy, finHoy);
		int bajoStock = productoRepository.findProductosConBajoStock().size();

		BigDecimal ventasDelMes = ventaPresencialRepository.findAll().stream()
				.filter(venta -> venta.getFecha() != null && YearMonth.from(venta.getFecha()).equals(mesActual))
				.map(venta -> venta.getTotal() == null ? BigDecimal.ZERO : venta.getTotal())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new AdminDashboardDTO(pedidosHoy, pendientes, ventasPresencialesHoy, bajoStock, ventasDelMes);
	}
}
