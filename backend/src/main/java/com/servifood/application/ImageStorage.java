package com.servifood.application;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {
    String store(MultipartFile file);
    StoredImage read(String internalName);
    record StoredImage(byte[] content, String contentType) {}
}
