package com.analistas.electrodental.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.analistas.electrodental.model.domain.Cliente;

public interface IClienteRepository extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findByEmailIgnoreCase(String email);

	@Query("""
			select c from Cliente c
			where replace(replace(replace(coalesce(c.dniCuit, ''), '.', ''), '-', ''), ' ', '') = :dniCuit
			""")
	Optional<Cliente> findByDniCuitNormalizado(@Param("dniCuit") String dniCuit);

	@Query("""
			select c from Cliente c
			where replace(replace(replace(coalesce(c.dniCuit, ''), '.', ''), '-', ''), ' ', '') like concat('%', :termino, '%')
			   or lower(coalesce(c.nombre, '')) like lower(concat('%', :termino, '%'))
			   or lower(coalesce(c.apellidoRazonSocial, '')) like lower(concat('%', :termino, '%'))
			order by c.nombre asc, c.apellidoRazonSocial asc
			""")
	List<Cliente> buscarSugerencias(@Param("termino") String termino, Pageable pageable);
}
