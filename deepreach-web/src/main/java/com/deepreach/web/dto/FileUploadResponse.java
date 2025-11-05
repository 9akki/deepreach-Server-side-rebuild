package com.deepreach.web.dto;

import lombok.Data;

/**
 * 文件上传响应数据
 */
@Data
public class FileUploadResponse {
    private String originalFileName;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private long size;
    private String contentType;
}

