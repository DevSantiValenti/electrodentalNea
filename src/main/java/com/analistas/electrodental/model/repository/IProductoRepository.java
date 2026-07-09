package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.Producto;

public interface IProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findAllByOrderByNombreAsc();

	@Query("""
			select p from Producto p
			left join fetch p.categoria c
			left join fetch p.subcategoria s
			where (p.eliminado is null or p.eliminado = false)
			order by coalesce(c.nombre, 'Sin categoria') asc,
			         coalesce(s.nombre, 'Sin subcategoria') asc,
			         p.nombre asc
			""")
	List<Producto> findProductosParaExcel();

	@Query("""
			select p from Producto p
			where (p.eliminado is null or p.eliminado = false or p.activo = true)
			order by p.nombre asc
			""")
	List<Producto> findAllNoEliminadosOrderByNombreAsc();

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

	@Query("""
			select p from Producto p
			where (p.eliminado is null or p.eliminado = false or p.activo = true)
			  and (:categoriaId is null or p.categoria.id = :categoriaId)
			  and (:subcategoriaId is null or p.subcategoria.id = :subcategoriaId)
			  and (:soloBajoStock = false or coalesce(p.stockWeb, 0) <= coalesce(p.stockMinimo, 0))
			  and (:soloOfertas = false or coalesce(p.oferta, false) = true)
			order by p.nombre asc
			""")
	List<Producto> filtrarAdmin(
			@Param("categoriaId") Long categoriaId,
			@Param("subcategoriaId") Long subcategoriaId,
			@Param("soloBajoStock") boolean soloBajoStock,
			@Param("soloOfertas") boolean soloOfertas);

	@Query("""
			select count(p) from Producto p
			where (p.eliminado is null or p.eliminado = false or p.activo = true)
			  and coalesce(p.stockWeb, 0) <= coalesce(p.stockMinimo, 0)
			""")
	long contarBajoStockAdmin();

	@Query("""
			select count(p) from Producto p
			where (p.eliminado is null or p.eliminado = false or p.activo = true)
			  and coalesce(p.oferta, false) = true
			""")
	long contarOfertasAdmin();

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Producto p set p.subcategoria = null where p.subcategoria.id = :subcategoriaId")
	int desvincularSubcategoria(@Param("subcategoriaId") Long subcategoriaId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Producto p set p.subcategoria = null where p.subcategoria.categoria.id = :categoriaId")
	int desvincularSubcategoriasDeCategoria(@Param("categoriaId") Long categoriaId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Producto p set p.categoria = null where p.categoria.id = :categoriaId")
	int desvincularCategoria(@Param("categoriaId") Long categoriaId);
}
