package com.analistas.electrodental.model.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analistas.electrodental.model.domain.CertificadoModo;
import com.analistas.electrodental.model.domain.Curso;
import com.analistas.electrodental.model.domain.CursoClase;
import com.analistas.electrodental.model.repository.ICursoRepository;

@Service
@Transactional(readOnly = true)
public class CursoServiceImpl implements ICursoService {

	private final ICursoRepository cursoRepository;

	public CursoServiceImpl(ICursoRepository cursoRepository) {
		this.cursoRepository = cursoRepository;
	}

	@Override
	public List<Curso> listarTodos() {
		return cursoRepository.findAllByOrderByOrdenAscTituloAsc();
	}

	@Override
	public List<Curso> listarActivos() {
		return cursoRepository.findByActivoTrueOrderByOrdenAscTituloAsc();
	}

	@Override
	public Optional<Curso> buscarPorId(Long id) {
		return cursoRepository.findById(id);
	}

	@Override
	public Optional<Curso> buscarActivoPorSlug(String slug) {
		Optional<Curso> curso = cursoRepository.findBySlugAndActivoTrue(slug);
		if (curso.isPresent()) {
			return curso;
		}
		String slugNormalizado = Curso.normalizarSlug(slug);
		if (slugNormalizado.isBlank() || slugNormalizado.equals(slug)) {
			return Optional.empty();
		}
		return cursoRepository.findByActivoTrueOrderByOrdenAscTituloAsc().stream()
				.filter(candidato -> slugNormalizado.equals(candidato.getSlugUrl()))
				.findFirst();
	}

	@Override
	@Transactional
	public Curso guardar(Curso curso) {
		if (curso.getId() == null) {
			normalizarCurso(curso);
			return cursoRepository.save(curso);
		}
		Curso actual = cursoRepository.findById(curso.getId())
				.orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + curso.getId()));
		actual.setTitulo(curso.getTitulo());
		actual.setSlug(curso.getSlug());
		actual.setDescripcionBreve(curso.getDescripcionBreve());
		actual.setMiniatura(curso.getMiniatura());
		actual.setDuracionHoras(curso.getDuracionHoras());
		actual.setActivo(curso.getActivo());
		actual.setOrden(curso.getOrden());
		actual.setCertificadoModo(curso.getCertificadoModo());
		actual.setCertificadoCodigo(curso.getCertificadoCodigo());
		actual.setCertificadoArchivo(curso.getCertificadoArchivo());
		actual.setCertificadoLinkExterno(curso.getCertificadoLinkExterno());
		actual.reemplazarClases(curso.getClases());
		normalizarCurso(actual);
		return cursoRepository.save(actual);
	}

	@Override
	@Transactional
	public void desactivar(Long id) {
		cursoRepository.findById(id).ifPresent(curso -> {
			curso.setActivo(false);
			cursoRepository.save(curso);
		});
	}

	@Override
	public long contarActivos() {
		return cursoRepository.contarActivos();
	}

	@Override
	public long contarClasesActivas() {
		return cursoRepository.findAll().stream()
				.flatMap(curso -> curso.getClases().stream())
				.filter(CursoClase::activoVisible)
				.count();
	}

	private void normalizarCurso(Curso curso) {
		if (curso.getCertificadoModo() == null) {
			curso.setCertificadoModo(CertificadoModo.PUBLICO);
		}
		List<CursoClase> clasesOrdenadas = curso.getClases().stream()
				.sorted(Comparator.comparing(CursoClase::ordenSeguro).thenComparing(CursoClase::tituloSeguro))
				.toList();
		curso.reemplazarClases(clasesOrdenadas);
	}
}
