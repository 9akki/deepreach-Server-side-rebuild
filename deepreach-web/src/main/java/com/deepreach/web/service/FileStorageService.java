package com.deepreach.web.service;

import com.deepreach.web.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * 存储上传文件并返回文件信息
     *
     * @param file 上传的文件
     * @return 文件信息
     */
    FileUploadResponse store(MultipartFile file);

    /**
     * 根据文件名删除文件
     *
     * @param fileName 存储时生成的文件名
     * @return 是否删除成功
     */
    boolean delete(String fileName);
}
