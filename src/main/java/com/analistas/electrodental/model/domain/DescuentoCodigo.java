package com.analistas.electrodental.model.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "descuentos_codigos")
@Getter
@Setter
@NoArgsConstructor
public class DescuentoCodigo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String codigo;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private TipoAplicacionDescuento tipoAplicacion = TipoAplicacionDescuento.CARRITO;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "descuento_productos",
			joinColumns = @JoinColumn(name = "descuento_id"),
			inverseJoinColumns = @JoinColumn(name = "producto_id"))
	private Set<Producto> productos = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "descuento_categorias",
			joinColumns = @JoinColumn(name = "descuento_id"),
			inverseJoinColumns = @JoinColumn(name = "categoria_id"))
	private Set<Categoria> categorias = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "descuento_subcategorias",
			joinColumns = @JoinColumn(name = "descuento_id"),
			inverseJoinColumns = @JoinColumn(name = "subcategoria_id"))
	private Set<Subcategoria> subcategorias = new HashSet<>();

	private LocalDate fechaInicio;

	private LocalDate fechaVencimiento;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal montoMinimoCompra = BigDecimal.ZERO;

	private Integer cantidadMaximaUsos;

	@Column(nullable = false)
	private Integer usosActuales = 0;

	@Column(nullable = false)
	private Boolean activo = true;

	private LocalDateTime fechaCreacion;

	private LocalDateTime fechaActualizacion;

	@PrePersist
	public void prePersist() {
		fechaCreacion = LocalDateTime.now();
		fechaActualizacion = fechaCreacion;
		normalizar();
	}

	@PreUpdate
	public void preUpdate() {
		fechaActualizacion = LocalDateTime.now();
		normalizar();
	}

	public void normalizar() {
		codigo = codigo == null ? "" : codigo.trim().toUpperCase();
		porcentajeDescuento = porcentajeDescuento == null ? BigDecimal.ZERO : porcentajeDescuento;
		montoMinimoCompra = montoMinimoCompra == null ? BigDecimal.ZERO : montoMinimoCompra;
		usosActuales = usosActuales == null ? 0 : Math.max(0, usosActuales);
		activo = activo != null && activo;
		if (tipoAplicacion == null) {
			tipoAplicacion = TipoAplicacionDescuento.CARRITO;
		}
	}

	public boolean tieneLimiteUsos() {
		return cantidadMaximaUsos != null && cantidadMaximaUsos > 0;
	}

	public boolean tieneUsosDisponibles() {
		int usos = usosActuales == null ? 0 : usosActuales;
		return !tieneLimiteUsos() || usos < cantidadMaximaUsos;
	}

	public boolean usado() {
		return usosActuales != null && usosActuales > 0;
	}
}
