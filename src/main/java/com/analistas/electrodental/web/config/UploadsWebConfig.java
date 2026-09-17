package com.analistas.electrodental.web.config;

import java.time.Duration;
import java.nio.file.Path;

import jakarta.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

	private static final CacheControl STATIC_CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

	private final Path uploadDir;

	public UploadsWebConfig(@Value("${electrodental.upload-dir:uploads}") String uploadDir) {
		this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String uploadLocation = uploadDir.toUri().toString();
		if (!uploadLocation.endsWith("/")) {
			uploadLocation += "/";
		}
		registry.addResourceHandler("/uploads/productos/**")
				.addResourceLocations(uploadLocation + "productos/")
				.setCacheControl(STATIC_CACHE);
		registry.addResourceHandler("/uploads/logo/**")
				.addResourceLocations(uploadLocation + "logo/")
				.setCacheControl(STATIC_CACHE);
		registry.addResourceHandler("/uploads/fondo/**")
				.addResourceLocations(uploadLocation + "fondo/")
				.setCacheControl(STATIC_CACHE);
		registry.addResourceHandler("/uploads/cursos/miniaturas/**")
				.addResourceLocations(uploadLocation + "cursos/miniaturas/")
				.setCacheControl(STATIC_CACHE);
		registry.addResourceHandler("/css/**")
				.addResourceLocations("classpath:/static/css/")
				.setCacheControl(STATIC_CACHE)
				.resourceChain(true);
		registry.addResourceHandler("/img/**")
				.addResourceLocations("classpath:/static/img/")
				.setCacheControl(STATIC_CACHE)
				.resourceChain(true);
		registry.addResourceHandler("/js/**")
				.addResourceLocations("classpath:/static/js/")
				.setCacheControl(STATIC_CACHE)
				.resourceChain(true);
		registry.addResourceHandler("/images/**")
				.addResourceLocations("classpath:/static/images/")
				.setCacheControl(STATIC_CACHE)
				.resourceChain(true);
	}

	@Bean
	MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofMegabytes(10));
		factory.setMaxRequestSize(DataSize.ofMegabytes(60));
		return factory.createMultipartConfig();
	}
}
