package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.Producto;

public interface IProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByActivoTrueOrderByNombreAsc();

	List<Producto> findByActivoTrueAndCategoriaSlugOrderByNombreAsc(String categoriaSlug);

	List<Producto> findByActivoTrueAndSubcategoriaSlugOrderByNombreAsc(String subcategoriaSlug);

	List<Producto> findByActivoTrueAndDestacadoTrueOrderByNombreAsc();

	List<Producto> findByActivoTrueAndOfertaTrueOrderByNombreAsc();

	List<Producto> findTop4ByActivoTrueAndCategoriaIdAndIdNotOrderByNombreAsc(Long categoriaId, Long productoId);

	Optional<Producto> findBySlugAndActivoTrue(String slug);

	@Query("""
			select p from Producto p
			where p.activo = true
			  and (
				lower(p.nombre) like lower(concat('%', :termino, '%'))
				or lower(coalesce(p.marca, '')) like lower(concat('%', :termino, '%'))
			  )
			order by p.nombre asc
			""")
	List<Producto> buscarSugerencias(@Param("termino") String termino, Pageable pageable);

	@Query("select p from Producto p where p.activo = true and p.stockWeb <= p.stockMinimo order by p.nombre asc")
	List<Producto> findProductosConBajoStock();
}
