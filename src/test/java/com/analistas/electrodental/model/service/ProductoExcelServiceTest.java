package com.analistas.electrodental.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.analistas.electrodental.model.domain.Categoria;
import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.Subcategoria;
import com.analistas.electrodental.model.domain.dto.ProductoExcelImportPreview;
import com.analistas.electrodental.model.repository.IProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoExcelServiceTest {

	@Mock
	private IProductoRepository productoRepository;

	@InjectMocks
	private ProductoExcelService productoExcelService;

	@Test
	void exportaYPrevisualizaCambiosDesdeElMismoFormato() throws Exception {
		Producto producto = producto();
		when(productoRepository.findProductosParaExcel()).thenReturn(List.of(producto));

		byte[] exportado = productoExcelService.exportarProductos();
		ByteArrayOutputStream modificado = new ByteArrayOutputStream();
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(exportado))) {
			Row row = workbook.getSheetAt(0).getRow(1);
			row.getCell(3).setCellValue("Turbina actualizada");
			row.getCell(5).setCellValue(8);
			row.getCell(7).setCellValue(1500.50);
			workbook.write(modificado);
		}

		MockMultipartFile archivo = new MockMultipartFile(
				"archivo",
				"productos.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				modificado.toByteArray());
		ProductoExcelImportPreview preview = productoExcelService.previsualizar(archivo);

		assertThat(preview.errores()).isEmpty();
		assertThat(preview.eliminaciones()).isEmpty();
		assertThat(preview.actualizaciones()).hasSize(1);
		assertThat(preview.actualizaciones().getFirst().cambios())
				.extracting(ProductoExcelImportPreview.FieldChange::campo)
				.containsExactly("Nombre", "Stock web", "Precio");
	}

	private Producto producto() {
		Categoria categoria = new Categoria();
		categoria.setId(10L);
		categoria.setNombre("Equipamiento");
		Subcategoria subcategoria = new Subcategoria();
		subcategoria.setId(20L);
		subcategoria.setNombre("Turbinas");
		subcategoria.setCategoria(categoria);

		Producto producto = new Producto();
		producto.setId(1L);
		producto.setNombre("Turbina NSK");
		producto.setMarca("NSK");
		producto.setCategoria(categoria);
		producto.setSubcategoria(subcategoria);
		producto.setStockWeb(5);
		producto.setStockFisico(12);
		producto.setPrecio(new BigDecimal("1000.00"));
		return producto;
	}
}
