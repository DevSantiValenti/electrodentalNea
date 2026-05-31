package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.analistas.electrodental.model.domain.Producto;

public interface IProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByActivoTrueOrderByNombreAsc();

	List<Producto> findByActivoTrueAndCategoriaSlugOrderByNombreAsc(String categoriaSlug);

	List<Producto> findByActivoTrueAndSubcategoriaSlugOrderByNombreAsc(String subcategoriaSlug);

	List<Producto> findByActivoTrueAndDestacadoTrueOrderByNombreAsc();

	List<Producto> findByActivoTrueAndOfertaTrueOrderByNombreAsc();

	List<Producto> findTop4ByActivoTrueAndCategoriaIdAndIdNotOrderByNombreAsc(Long categoriaId, Long productoId);

	Optional<Producto> findBySlugAndActivoTrue(String slug);

	@Query("select p from Producto p where p.activo = true and p.stockWeb <= p.stockMinimo order by p.nombre asc")
	List<Producto> findProductosConBajoStock();
}
