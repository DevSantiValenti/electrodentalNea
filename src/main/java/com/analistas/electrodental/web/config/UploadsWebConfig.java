package com.analistas.electrodental.web.config;

import java.nio.file.Path;

import jakarta.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

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
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadLocation);
	}

	@Bean
	MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofMegabytes(10));
		factory.setMaxRequestSize(DataSize.ofMegabytes(60));
		return factory.createMultipartConfig();
	}
}
