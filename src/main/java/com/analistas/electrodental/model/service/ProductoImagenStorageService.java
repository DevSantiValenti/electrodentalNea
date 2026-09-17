package com.analistas.electrodental.model.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductoImagenStorageService {

	private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif");
	private static final String PRODUCTOS_URL_PREFIX = "/uploads/productos/";
	private static final String PRODUCTOS_THUMBS_DIR = "thumbs";
	private static final String PRODUCTOS_THUMBS_URL_PREFIX = "/uploads/productos/thumbs/";
	private static final String CURSOS_MINIATURAS_URL_PREFIX = "/uploads/cursos/miniaturas/";
	public static final long CURSO_CERTIFICADO_MAX_BYTES = 5L * 1024L * 1024L;
	public static final String CURSO_CERTIFICADO_MAX_LABEL = "5 MB";
	private static final int PRODUCTO_MAX_DIMENSION = 1200;
	private static final int PRODUCTO_CARD_MAX_DIMENSION = 440;
	private static final float PRODUCTO_JPG_QUALITY = 0.82f;
	private static final float PRODUCTO_CARD_JPG_QUALITY = 0.70f;

	private final Path productosDir;
	private final Path logoDir;
	private final Path fondoDir;
	private final Path cursosMiniaturasDir;
	private final Path cursosCertificadosDir;

	public ProductoImagenStorageService(@Value("${electrodental.upload-dir:uploads}") String uploadDir) {
		Path baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
		this.productosDir = baseDir.resolve("productos");
		this.logoDir = baseDir.resolve("logo");
		this.fondoDir = baseDir.resolve("fondo");
		this.cursosMiniaturasDir = baseDir.resolve("cursos").resolve("miniaturas");
		this.cursosCertificadosDir = baseDir.resolve("cursos").resolve("certificados");
	}

	public String guardar(MultipartFile archivo) {
		return guardarProductoOptimizado(archivo);
	}

	public String guardarLogo(MultipartFile archivo) {
		return guardarEnDirectorio(archivo, logoDir, "/uploads/logo/");
	}

	public String guardarFondo(MultipartFile archivo) {
		return guardarEnDirectorio(archivo, fondoDir, "/uploads/fondo/");
	}

	public String guardarCursoMiniatura(MultipartFile archivo) {
		return guardarEnDirectorio(archivo, cursosMiniaturasDir, CURSOS_MINIATURAS_URL_PREFIX);
	}

	public String guardarCursoCertificado(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			return "";
		}
		validarPdf(archivo);
		try {
			Files.createDirectories(cursosCertificadosDir);
			String nombreArchivo = UUID.randomUUID() + ".pdf";
			Path destino = cursosCertificadosDir.resolve(nombreArchivo).normalize();
			if (!destino.startsWith(cursosCertificadosDir)) {
				throw new IllegalArgumentException("Nombre de archivo inválido.");
			}
			try (InputStream inputStream = archivo.getInputStream()) {
				Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
			}
			return "/uploads/cursos/certificados/" + nombreArchivo;
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo guardar el certificado subido.", ex);
		}
	}

	public Path resolverCursoCertificado(String archivoUrl) {
		String prefix = "/uploads/cursos/certificados/";
		if (!StringUtils.hasText(archivoUrl) || !archivoUrl.startsWith(prefix)) {
			throw new IllegalArgumentException("Certificado inválido.");
		}
		String nombreArchivo = archivoUrl.substring(prefix.length());
		if (nombreArchivo.contains("/") || nombreArchivo.contains("\\") || nombreArchivo.isBlank()) {
			throw new IllegalArgumentException("Certificado inválido.");
		}
		Path archivo = cursosCertificadosDir.resolve(nombreArchivo).normalize();
		if (!archivo.startsWith(cursosCertificadosDir)) {
			throw new IllegalArgumentException("Certificado inválido.");
		}
		return archivo;
	}

	public String cardUrl(String url) {
		try {
			if (!StringUtils.hasText(url) || !url.startsWith(PRODUCTOS_URL_PREFIX) || url.startsWith(PRODUCTOS_THUMBS_URL_PREFIX)) {
				return url;
			}
			String nombreArchivo = url.substring(PRODUCTOS_URL_PREFIX.length());
			if (nombreArchivo.contains("/") || nombreArchivo.contains("\\") || nombreArchivo.isBlank()) {
				return url;
			}
			String nombreThumb = nombreThumbnail(nombreArchivo);
			if (nombreThumb.isBlank()) {
				return url;
			}
			Path thumb = productosDir.resolve(PRODUCTOS_THUMBS_DIR).resolve(nombreThumb).normalize();
			if (Files.isRegularFile(thumb)) {
				return PRODUCTOS_THUMBS_URL_PREFIX + nombreThumb;
			}
			Path original = productosDir.resolve(nombreArchivo).normalize();
			if (!original.startsWith(productosDir) || !Files.isRegularFile(original)) {
				return url;
			}
			crearThumbnailSiHaceFalta(original, thumb);
			return Files.isRegularFile(thumb) ? PRODUCTOS_THUMBS_URL_PREFIX + nombreThumb : url;
		} catch (RuntimeException ex) {
			return url;
		}
	}

	private String guardarProductoOptimizado(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			return "";
		}
		validarImagen(archivo);
		String extension = obtenerExtension(archivo);
		try {
			Files.createDirectories(productosDir);
			String baseName = UUID.randomUUID().toString();
			if ("gif".equals(extension)) {
				return guardarOriginal(archivo, productosDir, PRODUCTOS_URL_PREFIX, baseName, extension);
			}
			BufferedImage imagenOriginal;
			try (InputStream inputStream = archivo.getInputStream()) {
				imagenOriginal = ImageIO.read(inputStream);
			}
			if (imagenOriginal == null) {
				return guardarOriginal(archivo, productosDir, PRODUCTOS_URL_PREFIX, baseName, extension);
			}
			String nombreArchivo = baseName + ".jpg";
			Path destino = productosDir.resolve(nombreArchivo).normalize();
			if (!destino.startsWith(productosDir)) {
				throw new IllegalArgumentException("Nombre de archivo inválido.");
			}
			BufferedImage optimizada = redimensionar(imagenOriginal, PRODUCTO_MAX_DIMENSION);
			escribirJpg(optimizada, destino, PRODUCTO_JPG_QUALITY);
			crearThumbnail(baseName, imagenOriginal);
			return PRODUCTOS_URL_PREFIX + nombreArchivo;
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo guardar la imagen subida.", ex);
		}
	}

	private String guardarEnDirectorio(MultipartFile archivo, Path directorio, String urlPrefix) {
		if (archivo == null || archivo.isEmpty()) {
			return "";
		}
		validarImagen(archivo);
		String extension = obtenerExtension(archivo);
		try {
			Files.createDirectories(directorio);
			return guardarOriginal(archivo, directorio, urlPrefix, UUID.randomUUID().toString(), extension);
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo guardar la imagen subida.", ex);
		}
	}

	private void validarImagen(MultipartFile archivo) {
		String contentType = archivo.getContentType();
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			throw new IllegalArgumentException("Solo se pueden subir archivos de imagen.");
		}
		String extension = obtenerExtension(archivo);
		if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
			throw new IllegalArgumentException("Formato de imagen no permitido. Usá JPG, PNG, WEBP o GIF.");
		}
	}

	private void validarPdf(MultipartFile archivo) {
		if (archivo.getSize() > CURSO_CERTIFICADO_MAX_BYTES) {
			throw new IllegalArgumentException("El certificado PDF no puede superar los " + CURSO_CERTIFICADO_MAX_LABEL + ".");
		}
		String contentType = archivo.getContentType();
		String extension = obtenerExtension(archivo);
		if (!"pdf".equals(extension) && (contentType == null || !"application/pdf".equalsIgnoreCase(contentType))) {
			throw new IllegalArgumentException("Solo se pueden subir certificados en PDF.");
		}
	}

	private String guardarOriginal(MultipartFile archivo, Path directorio, String urlPrefix, String baseName, String extension) throws IOException {
		String nombreArchivo = baseName + "." + extension;
		Path destino = directorio.resolve(nombreArchivo).normalize();
		if (!destino.startsWith(directorio)) {
			throw new IllegalArgumentException("Nombre de archivo inválido.");
		}
		try (InputStream inputStream = archivo.getInputStream()) {
			Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
		}
		return urlPrefix + nombreArchivo;
	}

	private void crearThumbnail(String baseName, BufferedImage imagenOriginal) {
		try {
			Path directorioThumbs = productosDir.resolve(PRODUCTOS_THUMBS_DIR);
			Files.createDirectories(directorioThumbs);
			Path destino = directorioThumbs.resolve(baseName + "-card.jpg").normalize();
			if (!destino.startsWith(directorioThumbs)) {
				return;
			}
			BufferedImage miniatura = redimensionar(imagenOriginal, PRODUCTO_CARD_MAX_DIMENSION);
			escribirJpg(miniatura, destino, PRODUCTO_CARD_JPG_QUALITY);
		} catch (IOException ignored) {
			// Si falla la miniatura, se conserva la imagen principal optimizada.
		}
	}

	private void crearThumbnailSiHaceFalta(Path original, Path thumb) {
		if (Files.isRegularFile(thumb)) {
			return;
		}
		try {
			Files.createDirectories(thumb.getParent());
			BufferedImage imagenOriginal = ImageIO.read(original.toFile());
			if (imagenOriginal == null) {
				return;
			}
			BufferedImage miniatura = redimensionar(imagenOriginal, PRODUCTO_CARD_MAX_DIMENSION);
			escribirJpg(miniatura, thumb, PRODUCTO_CARD_JPG_QUALITY);
		} catch (IOException ignored) {
			// Si la imagen anterior no puede optimizarse, se sirve la URL original.
		}
	}

	private String nombreThumbnail(String nombreArchivo) {
		int indicePunto = nombreArchivo.lastIndexOf('.');
		if (indicePunto <= 0) {
			return "";
		}
		String extension = nombreArchivo.substring(indicePunto + 1).toLowerCase(Locale.ROOT);
		if ("gif".equals(extension)) {
			return "";
		}
		return nombreArchivo.substring(0, indicePunto) + "-card.jpg";
	}

	private BufferedImage redimensionar(BufferedImage origen, int maxDimension) {
		int ancho = origen.getWidth();
		int alto = origen.getHeight();
		double escala = Math.min(1d, (double) maxDimension / Math.max(ancho, alto));
		int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
		int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));
		BufferedImage destino = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = destino.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, nuevoAncho, nuevoAlto);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawImage(origen, 0, 0, nuevoAncho, nuevoAlto, null);
		} finally {
			g.dispose();
		}
		return destino;
	}

	private void escribirJpg(BufferedImage imagen, Path destino, float calidad) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
		if (!writers.hasNext()) {
			throw new IOException("No hay escritor JPG disponible.");
		}
		ImageWriter writer = writers.next();
		try (OutputStream outputStream = Files.newOutputStream(destino);
				ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
			writer.setOutput(imageOutputStream);
			ImageWriteParam params = writer.getDefaultWriteParam();
			if (params.canWriteCompressed()) {
				params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				params.setCompressionQuality(calidad);
			}
			writer.write(null, new IIOImage(imagen, null, null), params);
		} finally {
			writer.dispose();
		}
	}

	private String obtenerExtension(MultipartFile archivo) {
		String nombreOriginal = StringUtils.cleanPath(archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename());
		int indicePunto = nombreOriginal.lastIndexOf('.');
		if (indicePunto >= 0 && indicePunto < nombreOriginal.length() - 1) {
			return nombreOriginal.substring(indicePunto + 1).toLowerCase(Locale.ROOT);
		}
		return switch (archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT)) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			case "image/gif" -> "gif";
			default -> "";
		};
	}
}
