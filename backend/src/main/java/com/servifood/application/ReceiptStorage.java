package com.servifood.application;

import org.springframework.web.multipart.MultipartFile;

public interface ReceiptStorage {
    String store(MultipartFile file);
    void deleteQuietly(String storedPath);
}
