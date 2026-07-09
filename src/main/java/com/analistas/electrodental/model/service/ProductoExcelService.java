package com.analistas.electrodental.model.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.analistas.electrodental.model.domain.Producto;
import com.analistas.electrodental.model.domain.dto.ProductoExcelImportPreview;
import com.analistas.electrodental.model.repository.IProductoRepository;

@Service
public class ProductoExcelService {

	private static final String SHEET_PRODUCTOS = "Productos";
	private static final String[] HEADERS = { "ID", "Categoria", "Subcategoria", "Nombre", "Marca", "Stock web", "Stock fisico", "Precio" };
	private static final int COL_ID = 0;
	private static final int COL_CATEGORIA = 1;
	private static final int COL_SUBCATEGORIA = 2;
	private static final int COL_NOMBRE = 3;
	private static final int COL_MARCA = 4;
	private static final int COL_STOCK_WEB = 5;
	private static final int COL_STOCK_FISICO = 6;
	private static final int COL_PRECIO = 7;

	private final IProductoRepository productoRepository;

	public ProductoExcelService(IProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	@Transactional(readOnly = true)
	public byte[] exportarProductos() {
		return exportarProductos(null, null, false, false);
	}

	@Transactional(readOnly = true)
	public byte[] exportarProductos(Long categoriaId, Long subcategoriaId, boolean soloBajoStock) {
		return exportarProductos(categoriaId, subcategoriaId, soloBajoStock, false);
	}

	@Transactional(readOnly = true)
	public byte[] exportarProductos(Long categoriaId, Long subcategoriaId, boolean soloBajoStock, boolean soloOfertas) {
		List<Producto> productos = soloBajoStock || soloOfertas
				? productoRepository.filtrarAdmin(categoriaId, subcategoriaId, soloBajoStock, soloOfertas)
				: productoRepository.findProductosParaExcel();
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(SHEET_PRODUCTOS);
			Styles styles = crearStyles(workbook);
			crearHeader(sheet, styles.header());
			int rowIndex = 1;
			for (Producto producto : productos) {
				Row row = sheet.createRow(rowIndex++);
				escribirProducto(row, producto, styles);
			}
			sheet.createFreezePane(0, 1);
			sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rowIndex - 1, 0), 0, HEADERS.length - 1));
			for (int col = 0; col < HEADERS.length; col++) {
				sheet.autoSizeColumn(col);
				sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + 700, 18000));
			}
			workbook.write(output);
			return output.toByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo generar el Excel de productos.", ex);
		}
	}

	@Transactional(readOnly = true)
	public ProductoExcelImportPreview previsualizar(MultipartFile archivo) {
		return previsualizar(archivo, false);
	}

	@Transactional(readOnly = true)
	public ProductoExcelImportPreview previsualizar(MultipartFile archivo, boolean importacionParcial) {
		if (archivo == null || archivo.isEmpty()) {
			throw new IllegalArgumentException("Seleccioná un archivo Excel para importar.");
		}
		List<String> errores = new ArrayList<>();
		List<ImportedRow> filasImportadas = leerFilas(archivo, errores);
		List<Producto> actuales = productoRepository.findProductosParaExcel();
		Map<Long, Producto> productosPorId = new HashMap<>();
		actuales.forEach(producto -> productosPorId.put(producto.getId(), producto));

		Set<Long> idsImportados = new HashSet<>();
		List<ProductoExcelImportPreview.Update> actualizaciones = new ArrayList<>();
		for (ImportedRow fila : filasImportadas) {
			if (!idsImportados.add(fila.productoId())) {
				errores.add("Fila " + fila.rowNumber() + ": el ID " + fila.productoId() + " está duplicado.");
				continue;
			}
			Producto producto = productosPorId.get(fila.productoId());
			if (producto == null) {
				errores.add("Fila " + fila.rowNumber() + ": no existe un producto activo/no eliminado con ID " + fila.productoId() + ".");
				continue;
			}
			List<ProductoExcelImportPreview.FieldChange> cambios = cambios(producto, fila);
			if (!cambios.isEmpty()) {
				actualizaciones.add(new ProductoExcelImportPreview.Update(
						producto.getId(),
						nombreCategoria(producto),
						nombreSubcategoria(producto),
						valorTexto(producto.getNombre()),
						fila.nombre(),
						fila.marca(),
						fila.stockWeb(),
						fila.stockFisico(),
						fila.precio(),
						cambios));
			}
		}

		List<ProductoExcelImportPreview.Deletion> eliminaciones = importacionParcial
				? List.of()
				: actuales.stream()
						.filter(producto -> !idsImportados.contains(producto.getId()))
						.map(producto -> new ProductoExcelImportPreview.Deletion(
								producto.getId(),
								nombreCategoria(producto),
								nombreSubcategoria(producto),
								valorTexto(producto.getNombre())))
						.toList();

		return new ProductoExcelImportPreview(filasImportadas.size(), actualizaciones, eliminaciones, errores, importacionParcial);
	}

	@Transactional
	public ProductoExcelImportResult aplicar(ProductoExcelImportPreview preview) {
		if (preview == null) {
			throw new IllegalArgumentException("No hay una previsualización de importación para confirmar.");
		}
		if (!preview.puedeConfirmar()) {
			throw new IllegalArgumentException("La importación tiene errores o no contiene cambios para aplicar.");
		}
		int actualizados = 0;
		for (ProductoExcelImportPreview.Update update : preview.actualizaciones()) {
			Producto producto = productoRepository.findById(update.productoId()).orElse(null);
			if (producto == null) {
				continue;
			}
			producto.setNombre(update.nombreNuevo());
			producto.setMarca(normalizarNullable(update.marcaNueva()));
			producto.setStockWeb(update.stockWebNuevo());
			producto.setStockFisico(update.stockFisicoNuevo());
			producto.setPrecio(update.precioNuevo());
			productoRepository.save(producto);
			actualizados++;
		}
		int eliminados = 0;
		for (ProductoExcelImportPreview.Deletion deletion : preview.eliminaciones()) {
			Producto producto = productoRepository.findById(deletion.productoId()).orElse(null);
			if (producto == null) {
				continue;
			}
			producto.setEliminado(true);
			producto.setActivo(false);
			producto.setCompraHabilitada(false);
			productoRepository.save(producto);
			eliminados++;
		}
		return new ProductoExcelImportResult(actualizados, eliminados);
	}

	private List<ImportedRow> leerFilas(MultipartFile archivo, List<String> errores) {
		try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
			if (workbook.getNumberOfSheets() == 0) {
				throw new IllegalArgumentException("El Excel no tiene hojas.");
			}
			Sheet sheet = workbook.getSheetAt(0);
			validarHeader(sheet.getRow(0));
			DataFormatter formatter = new DataFormatter();
			List<ImportedRow> filas = new ArrayList<>();
			for (int index = 1; index <= sheet.getLastRowNum(); index++) {
				Row row = sheet.getRow(index);
				if (row == null || filaVacia(row, formatter)) {
					continue;
				}
				int rowNumber = index + 1;
				try {
					Long productoId = leerLong(row, COL_ID, "ID", formatter);
					String nombre = leerTextoRequerido(row, COL_NOMBRE, "Nombre", formatter);
					String marca = leerTexto(row, COL_MARCA, formatter);
					Integer stockWeb = leerEntero(row, COL_STOCK_WEB, "Stock web", formatter);
					Integer stockFisico = leerEntero(row, COL_STOCK_FISICO, "Stock fisico", formatter);
					BigDecimal precio = leerDecimal(row, COL_PRECIO, "Precio", formatter);
					filas.add(new ImportedRow(rowNumber, productoId, nombre, marca, stockWeb, stockFisico, precio));
				} catch (IllegalArgumentException ex) {
					errores.add("Fila " + rowNumber + ": " + ex.getMessage());
				}
			}
			return filas;
		} catch (IOException ex) {
			throw new IllegalArgumentException("No se pudo leer el Excel. Revisá que sea un archivo .xlsx válido.", ex);
		}
	}

	private void validarHeader(Row header) {
		if (header == null) {
			throw new IllegalArgumentException("Formato inválido: falta la fila de encabezados.");
		}
		DataFormatter formatter = new DataFormatter();
		for (int col = 0; col < HEADERS.length; col++) {
			String esperado = normalizarHeader(HEADERS[col]);
			String recibido = normalizarHeader(formatter.formatCellValue(header.getCell(col)));
			if (!esperado.equals(recibido)) {
				throw new IllegalArgumentException("Formato inválido: descargá el Excel desde Exportar y mantené los encabezados originales.");
			}
		}
	}

	private List<ProductoExcelImportPreview.FieldChange> cambios(Producto producto, ImportedRow fila) {
		List<ProductoExcelImportPreview.FieldChange> cambios = new ArrayList<>();
		agregarCambioTexto(cambios, "Nombre", producto.getNombre(), fila.nombre());
		agregarCambioTexto(cambios, "Marca", producto.getMarca(), fila.marca());
		agregarCambioEntero(cambios, "Stock web", producto.getStockWeb(), fila.stockWeb());
		agregarCambioEntero(cambios, "Stock físico", producto.getStockFisico(), fila.stockFisico());
		agregarCambioDecimal(cambios, "Precio", producto.getPrecio(), fila.precio());
		return cambios;
	}

	private void agregarCambioTexto(List<ProductoExcelImportPreview.FieldChange> cambios, String campo, String actual, String nuevo) {
		if (!valorTexto(actual).equals(valorTexto(nuevo))) {
			cambios.add(new ProductoExcelImportPreview.FieldChange(campo, valorTexto(actual), valorTexto(nuevo)));
		}
	}

	private void agregarCambioEntero(List<ProductoExcelImportPreview.FieldChange> cambios, String campo, Integer actual, Integer nuevo) {
		Integer actualSeguro = actual == null ? 0 : actual;
		if (!actualSeguro.equals(nuevo)) {
			cambios.add(new ProductoExcelImportPreview.FieldChange(campo, actualSeguro.toString(), nuevo.toString()));
		}
	}

	private void agregarCambioDecimal(List<ProductoExcelImportPreview.FieldChange> cambios, String campo, BigDecimal actual, BigDecimal nuevo) {
		BigDecimal actualSeguro = dinero(actual);
		BigDecimal nuevoSeguro = dinero(nuevo);
		if (actualSeguro.compareTo(nuevoSeguro) != 0) {
			cambios.add(new ProductoExcelImportPreview.FieldChange(campo, formatoDecimal(actualSeguro), formatoDecimal(nuevoSeguro)));
		}
	}

	private void crearHeader(Sheet sheet, CellStyle headerStyle) {
		Row header = sheet.createRow(0);
		for (int col = 0; col < HEADERS.length; col++) {
			Cell cell = header.createCell(col);
			cell.setCellValue(HEADERS[col]);
			cell.setCellStyle(headerStyle);
		}
	}

	private void escribirProducto(Row row, Producto producto, Styles styles) {
		escribirNumero(row, COL_ID, producto.getId(), styles.integer());
		escribirTexto(row, COL_CATEGORIA, nombreCategoria(producto), styles.text());
		escribirTexto(row, COL_SUBCATEGORIA, nombreSubcategoria(producto), styles.text());
		escribirTexto(row, COL_NOMBRE, valorTexto(producto.getNombre()), styles.text());
		escribirTexto(row, COL_MARCA, valorTexto(producto.getMarca()), styles.text());
		escribirNumero(row, COL_STOCK_WEB, producto.getStockWeb(), styles.integer());
		escribirNumero(row, COL_STOCK_FISICO, producto.getStockFisico(), styles.integer());
		escribirDecimal(row, COL_PRECIO, producto.getPrecio(), styles.money());
	}

	private Styles crearStyles(Workbook workbook) {
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		CellStyle header = workbook.createCellStyle();
		header.setFont(headerFont);
		header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		header.setBorderBottom(BorderStyle.THIN);

		CellStyle text = workbook.createCellStyle();
		text.setBorderBottom(BorderStyle.HAIR);

		DataFormat dataFormat = workbook.createDataFormat();
		CellStyle integer = workbook.createCellStyle();
		integer.cloneStyleFrom(text);
		integer.setDataFormat(dataFormat.getFormat("#,##0"));

		CellStyle money = workbook.createCellStyle();
		money.cloneStyleFrom(text);
		money.setDataFormat(dataFormat.getFormat("$#,##0.00"));

		return new Styles(header, text, integer, money);
	}

	private void escribirTexto(Row row, int col, String valor, CellStyle style) {
		Cell cell = row.createCell(col, CellType.STRING);
		cell.setCellValue(valor);
		cell.setCellStyle(style);
	}

	private void escribirNumero(Row row, int col, Number valor, CellStyle style) {
		Cell cell = row.createCell(col, CellType.NUMERIC);
		cell.setCellValue(valor == null ? 0 : valor.doubleValue());
		cell.setCellStyle(style);
	}

	private void escribirDecimal(Row row, int col, BigDecimal valor, CellStyle style) {
		Cell cell = row.createCell(col, CellType.NUMERIC);
		cell.setCellValue(dinero(valor).doubleValue());
		cell.setCellStyle(style);
	}

	private boolean filaVacia(Row row, DataFormatter formatter) {
		for (int col = 0; col < HEADERS.length; col++) {
			if (!formatter.formatCellValue(row.getCell(col)).trim().isBlank()) {
				return false;
			}
		}
		return true;
	}

	private Long leerLong(Row row, int col, String campo, DataFormatter formatter) {
		BigDecimal valor = leerNumero(row, col, campo, formatter);
		try {
			return valor.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
		} catch (ArithmeticException ex) {
			throw new IllegalArgumentException(campo + " debe ser un número entero.");
		}
	}

	private Integer leerEntero(Row row, int col, String campo, DataFormatter formatter) {
		BigDecimal valor = leerNumero(row, col, campo, formatter);
		try {
			int entero = valor.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
			if (entero < 0) {
				throw new IllegalArgumentException(campo + " no puede ser negativo.");
			}
			return entero;
		} catch (ArithmeticException ex) {
			throw new IllegalArgumentException(campo + " debe ser un número entero.");
		}
	}

	private BigDecimal leerDecimal(Row row, int col, String campo, DataFormatter formatter) {
		BigDecimal valor = leerNumero(row, col, campo, formatter).setScale(2, RoundingMode.HALF_UP);
		if (valor.signum() < 0) {
			throw new IllegalArgumentException(campo + " no puede ser negativo.");
		}
		return valor;
	}

	private BigDecimal leerNumero(Row row, int col, String campo, DataFormatter formatter) {
		Cell cell = row.getCell(col);
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			throw new IllegalArgumentException(campo + " es obligatorio.");
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return BigDecimal.valueOf(cell.getNumericCellValue());
		}
		String valor = formatter.formatCellValue(cell).trim();
		if (valor.isBlank()) {
			throw new IllegalArgumentException(campo + " es obligatorio.");
		}
		return parsearDecimal(valor, campo);
	}

	private String leerTextoRequerido(Row row, int col, String campo, DataFormatter formatter) {
		String valor = leerTexto(row, col, formatter);
		if (valor.isBlank()) {
			throw new IllegalArgumentException(campo + " es obligatorio.");
		}
		return valor;
	}

	private String leerTexto(Row row, int col, DataFormatter formatter) {
		Cell cell = row.getCell(col);
		return cell == null ? "" : formatter.formatCellValue(cell).trim();
	}

	private BigDecimal parsearDecimal(String valor, String campo) {
		String normalizado = valor
				.replace("$", "")
				.replace("\u00A0", "")
				.replace(" ", "")
				.trim();
		if (normalizado.contains(",") && normalizado.contains(".")) {
			normalizado = normalizado.replace(".", "").replace(",", ".");
		} else {
			normalizado = normalizado.replace(",", ".");
		}
		try {
			return new BigDecimal(normalizado);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(campo + " debe ser numérico.");
		}
	}

	private String nombreCategoria(Producto producto) {
		return producto.getCategoria() == null ? "Sin categoria" : valorTexto(producto.getCategoria().getNombre());
	}

	private String nombreSubcategoria(Producto producto) {
		return producto.getSubcategoria() == null ? "Sin subcategoria" : valorTexto(producto.getSubcategoria().getNombre());
	}

	private String valorTexto(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private String normalizarNullable(String valor) {
		String normalizado = valorTexto(valor);
		return normalizado.isBlank() ? null : normalizado;
	}

	private String normalizarHeader(String valor) {
		return valor == null ? "" : valor.trim().toLowerCase();
	}

	private BigDecimal dinero(BigDecimal valor) {
		return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
	}

	private String formatoDecimal(BigDecimal valor) {
		return dinero(valor).stripTrailingZeros().toPlainString();
	}

	private record Styles(CellStyle header, CellStyle text, CellStyle integer, CellStyle money) {
	}

	private record ImportedRow(
			int rowNumber,
			Long productoId,
			String nombre,
			String marca,
			Integer stockWeb,
			Integer stockFisico,
			BigDecimal precio) {
	}

	public record ProductoExcelImportResult(
			int productosActualizados,
			int productosEliminados) {
	}
}
