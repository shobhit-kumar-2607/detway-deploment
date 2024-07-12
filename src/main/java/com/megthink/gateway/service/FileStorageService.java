package com.megthink.gateway.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.megthink.gateway.utils.ReadConfigFile;

@Service
public class FileStorageService {

	private static final Logger _logger = LoggerFactory.getLogger(FileStorageService.class);

	@Autowired
	public FileStorageService() {
	}

	public String storeFile(MultipartFile file, String systemGeneratedFileName) {
		try {
			String filePath = System.getProperty("user.home") + "/"
					+ ReadConfigFile.getProperties().getProperty("upload.document") + "/" + systemGeneratedFileName;
			Path targetLocation = Paths.get(filePath);// this.fileStorageLocation.resolve(systemGeneratedFileName);
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
			return systemGeneratedFileName;
		} catch (IOException ex) {
			_logger.error("throwing error when we try to upload doc - " + ex);
		}
		return systemGeneratedFileName;
	}

	public Resource loadFileAsResource(String fileName) {
		try {
			String filePathString = System.getProperty("user.home") + "/"
					+ ReadConfigFile.getProperties().getProperty("upload.document") + "/" + fileName;
			Path filePath = Paths.get(filePathString);
			Resource resource = new UrlResource(filePath.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				System.out.println("File not found " + filePathString);
			}
		} catch (MalformedURLException ex) {
			System.out.println("File not found " + fileName);
		}
		return null;
	}

}