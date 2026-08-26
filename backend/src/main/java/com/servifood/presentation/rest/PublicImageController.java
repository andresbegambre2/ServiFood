package com.servifood.presentation.rest;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.servifood.application.ImageStorage;

@RestController
@RequestMapping("/api/v1/public/product-images")
public class PublicImageController {
    private final ImageStorage storage;
    public PublicImageController(ImageStorage storage) { this.storage = storage; }

    @GetMapping("/{name}")
    ResponseEntity<byte[]> image(@PathVariable String name) {
        var image = storage.read(name);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noCache()).header("X-Content-Type-Options", "nosniff").body(image.content());
    }
}
