package com.servifood.presentation.rest.dto;
import java.math.BigDecimal;
import java.util.List;
public record ProductDetailResponse(Long id, String name, String slug, String description, BigDecimal price,
        String imagePath, boolean available, boolean featured, CategoryResponse category, List<ExtraResponse> allowedExtras) {}
