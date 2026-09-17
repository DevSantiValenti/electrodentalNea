package com.analistas.electrodental.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ProductoImagenStorageServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void guardarProductoOptimizaImagenYGeneraMiniaturaParaCards() throws Exception {
		ProductoImagenStorageService service = new ProductoImagenStorageService(tempDir.toString());
		byte[] bytes = crearPng(1400, 1000);
		MockMultipartFile archivo = new MockMultipartFile("imagen", "producto.png", "image/png", bytes);

		String url = service.guardar(archivo);

		assertTrue(url.startsWith("/uploads/productos/"));
		assertTrue(url.endsWith(".jpg"));
		Path imagenOptimizada = tempDir.resolve("productos").resolve(Path.of(url).getFileName().toString());
		assertTrue(Files.isRegularFile(imagenOptimizada));
		BufferedImage optimizada = ImageIO.read(imagenOptimizada.toFile());
		assertNotNull(optimizada);
		assertEquals(1200, Math.max(optimizada.getWidth(), optimizada.getHeight()));

		String cardUrl = service.cardUrl(url);

		assertTrue(cardUrl.startsWith("/uploads/productos/thumbs/"));
		Path miniatura = tempDir.resolve("productos").resolve("thumbs").resolve(Path.of(cardUrl).getFileName().toString());
		assertTrue(Files.isRegularFile(miniatura));
		BufferedImage miniaturaImagen = ImageIO.read(miniatura.toFile());
		assertNotNull(miniaturaImagen);
		assertEquals(440, Math.max(miniaturaImagen.getWidth(), miniaturaImagen.getHeight()));
		assertTrue(cardUrl.endsWith("-card.jpg"));
	}

	@Test
	void generaMiniaturaParaProductoWebpAnterior() throws Exception {
		ProductoImagenStorageService service = new ProductoImagenStorageService(tempDir.toString());
		Path productosDir = tempDir.resolve("productos");
		Files.createDirectories(productosDir);
		Path webp = productosDir.resolve("producto-viejo.webp");
		BufferedImage imagen = crearImagen(900, 900);
		assertTrue(ImageIO.write(imagen, "webp", webp.toFile()));

		String cardUrl = service.cardUrl("/uploads/productos/producto-viejo.webp");

		assertEquals("/uploads/productos/thumbs/producto-viejo-card.jpg", cardUrl);
		Path miniatura = productosDir.resolve("thumbs").resolve("producto-viejo-card.jpg");
		assertTrue(Files.isRegularFile(miniatura));
		BufferedImage miniaturaImagen = ImageIO.read(miniatura.toFile());
		assertNotNull(miniaturaImagen);
		assertEquals(440, Math.max(miniaturaImagen.getWidth(), miniaturaImagen.getHeight()));
	}

	@Test
	void guardaCertificadoCursoComoPdfPrivadoResoluble() {
		ProductoImagenStorageService service = new ProductoImagenStorageService(tempDir.toString());
		MockMultipartFile archivo = new MockMultipartFile("certificado", "certificado.pdf", "application/pdf", "%PDF-1.4".getBytes());

		String url = service.guardarCursoCertificado(archivo);
		Path certificado = service.resolverCursoCertificado(url);

		assertTrue(url.startsWith("/uploads/cursos/certificados/"));
		assertTrue(url.endsWith(".pdf"));
		assertTrue(Files.isRegularFile(certificado));
		assertTrue(certificado.startsWith(tempDir.resolve("cursos").resolve("certificados")));
	}

	@Test
	void rechazaCertificadoCursoMayorA5Mb() {
		ProductoImagenStorageService service = new ProductoImagenStorageService(tempDir.toString());
		byte[] bytes = new byte[(int) ProductoImagenStorageService.CURSO_CERTIFICADO_MAX_BYTES + 1];
		MockMultipartFile archivo = new MockMultipartFile("certificado", "certificado.pdf", "application/pdf", bytes);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.guardarCursoCertificado(archivo));

		assertEquals("El certificado PDF no puede superar los 5 MB.", ex.getMessage());
	}

	private byte[] crearPng(int width, int height) throws Exception {
		BufferedImage imagen = crearImagen(width, height);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(imagen, "png", output);
		return output.toByteArray();
	}

	private BufferedImage crearImagen(int width, int height) {
		BufferedImage imagen = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = imagen.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.setColor(new Color(24, 119, 242));
			g.fillOval(width / 5, height / 5, width / 2, height / 2);
		} finally {
			g.dispose();
		}
		return imagen;
	}
}
