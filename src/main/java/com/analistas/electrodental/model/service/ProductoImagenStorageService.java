package com.analistas.electrodental.model.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductoImagenStorageService {

	private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif");

	private final Path productosDir;

	public ProductoImagenStorageService(@Value("${electrodental.upload-dir:uploads}") String uploadDir) {
		this.productosDir = Path.of(uploadDir).toAbsolutePath().normalize().resolve("productos");
	}

	public String guardar(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			return "";
		}
		String contentType = archivo.getContentType();
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			throw new IllegalArgumentException("Solo se pueden subir archivos de imagen.");
		}

		String extension = obtenerExtension(archivo);
		if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
			throw new IllegalArgumentException("Formato de imagen no permitido. Usá JPG, PNG, WEBP o GIF.");
		}

		try {
			Files.createDirectories(productosDir);
			String nombreArchivo = UUID.randomUUID() + "." + extension;
			Path destino = productosDir.resolve(nombreArchivo).normalize();
			if (!destino.startsWith(productosDir)) {
				throw new IllegalArgumentException("Nombre de archivo inválido.");
			}
			try (InputStream inputStream = archivo.getInputStream()) {
				Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
			}
			return "/uploads/productos/" + nombreArchivo;
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo guardar la imagen subida.", ex);
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
