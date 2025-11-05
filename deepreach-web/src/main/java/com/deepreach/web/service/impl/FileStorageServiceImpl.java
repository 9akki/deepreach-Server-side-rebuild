package com.deepreach.web.service.impl;

import com.deepreach.web.dto.FileUploadResponse;
import com.deepreach.web.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final String DEFAULT_UPLOAD_DIR = "uploads";
    private final Path rootLocation;

    public FileStorageServiceImpl() {
        this.rootLocation = Paths.get(DEFAULT_UPLOAD_DIR);
        initStorage();
    }

    @Override
    public FileUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String generatedName = UUID.randomUUID().toString().replaceAll("-", "");
        String storedFileName = extension.isEmpty() ? generatedName : generatedName + "." + extension;

        try {
            Path destinationFile = rootLocation.resolve(storedFileName).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath().normalize())) {
                throw new IllegalStateException("无法存储文件到目标路径：" + destinationFile);
            }
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            FileUploadResponse response = new FileUploadResponse();
            response.setOriginalFileName(originalFilename);
            response.setFileName(storedFileName);
            response.setFilePath(destinationFile.toAbsolutePath().toString());
            response.setFileUrl("/uploads/" + storedFileName);
            response.setSize(file.getSize());
            response.setContentType(file.getContentType());
            return response;
        } catch (IOException e) {
            log.error("文件保存失败: {}", originalFilename, e);
            throw new RuntimeException("保存文件失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        try {
            Path target = rootLocation.resolve(fileName).normalize().toAbsolutePath();
            if (!target.getParent().equals(rootLocation.toAbsolutePath().normalize())) {
                throw new IllegalStateException("非法的文件路径");
            }
            if (Files.notExists(target)) {
                return false;
            }
            Files.delete(target);
            return true;
        } catch (IOException e) {
            log.error("删除文件失败: {}", fileName, e);
            throw new RuntimeException("删除文件失败：" + e.getMessage(), e);
        }
    }

    private void initStorage() {
        try {
            Files.createDirectories(rootLocation.toAbsolutePath());
        } catch (IOException e) {
            log.error("初始化上传目录失败: {}", rootLocation, e);
            throw new RuntimeException("初始化上传目录失败：" + e.getMessage(), e);
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }
}
