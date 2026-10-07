package com.smarthostel.service;

import com.smarthostel.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Saves uploaded images to the local "uploads" folder.
 * Only the relative path (e.g. "complaints/3f2a....jpg") is stored in Oracle.
 */
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath();
    }

    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("No file was uploaded");
        }
        String extension = ALLOWED.get(file.getContentType());
        if (extension == null) {
            throw new BusinessRuleException("Only JPG, PNG or WEBP images are allowed");
        }
        String relativePath = folder + "/" + UUID.randomUUID() + extension;
        try {
            Path target = root.resolve(relativePath);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessRuleException("Could not save the image: " + e.getMessage());
        }
        return relativePath;
    }
}
