package com.deepreach.web.controller;

import com.deepreach.common.web.Result;
import com.deepreach.web.dto.FileUploadResponse;
import com.deepreach.web.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        try {
            FileUploadResponse response = fileStorageService.store(file);
            return Result.success("上传成功", response);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            log.error("文件上传失败", ex);
            return Result.error("文件上传失败: " + ex.getMessage());
        }
    }

    @GetMapping("/template/sms-import")
    public ResponseEntity<Resource> downloadSmsImportTemplate() throws IOException {
        Path filePath = fileStorageService.getUploadPath()
            .resolve("8bf34c966b2d4758856c90450e49081d.xlsx")
            .normalize();
        byte[] data = Files.readAllBytes(filePath);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sms_import_template-1.xlsx\"")
            .contentLength(data.length)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }

    @DeleteMapping
    public Result<Void> delete(@RequestParam(value = "fileName", required = false) String fileName,
                               @RequestParam(value = "filePath", required = false) String filePath,
                               @RequestParam(value = "fileUrl", required = false) String fileUrl) {
        String target = resolveTarget(fileName, filePath, fileUrl);
        if (!org.springframework.util.StringUtils.hasText(target)) {
            return Result.error(400, "文件名、文件路径或文件URL至少提供一个");
        }

        try {
            boolean deleted = fileStorageService.delete(target);
            if (!deleted) {
                return Result.error(404, "文件不存在");
            }
            return Result.success("删除成功", null);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            log.error("删除文件失败：{}", target, ex);
            return Result.error("删除文件失败: " + ex.getMessage());
        }
    }

    private String resolveTarget(String fileName, String filePath, String fileUrl) {
        String candidate = null;
        if (org.springframework.util.StringUtils.hasText(filePath)) {
            candidate = filePath;
        } else if (org.springframework.util.StringUtils.hasText(fileUrl)) {
            candidate = fileUrl;
        } else if (org.springframework.util.StringUtils.hasText(fileName)) {
            candidate = fileName;
        }

        if (!org.springframework.util.StringUtils.hasText(candidate)) {
            return null;
        }

        String normalized = candidate.trim();
        if (normalized.startsWith("/api")) {
            normalized = normalized.substring("/api".length());
        }
        if (normalized.startsWith("files/")) {
            normalized = normalized.substring("files/".length());
        }
        if (normalized.startsWith("/files/")) {
            normalized = normalized.substring("/files/".length());
        }
        if (normalized.startsWith("/uploads/")) {
            normalized = normalized.substring("/uploads/".length());
        } else if (normalized.startsWith("uploads/")) {
            normalized = normalized.substring("uploads/".length());
        }

        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // 如果已经是绝对路径，直接返回
        if (normalized.startsWith(System.getProperty("file.separator"))
            || normalized.matches("^[A-Za-z]:\\\\.*")) {
            return candidate;
        }

        // 处理相对路径，恢复成 uploads/xxx
        return normalized.contains("/")
            ? normalized
            : normalized; // fileStorageService.delete 会解析到 uploads 目录
    }
}
