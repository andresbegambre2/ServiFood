package com.servifood.application;

import org.springframework.web.multipart.MultipartFile;

public interface ReceiptStorage {
    String store(MultipartFile file);
    StoredFile read(String storedPath);
    void deleteQuietly(String storedPath);
    record StoredFile(byte[] content, String contentType) {}
}
