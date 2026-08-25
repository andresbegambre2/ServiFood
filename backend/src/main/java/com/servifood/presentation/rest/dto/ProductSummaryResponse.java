package com.servifood.presentation.rest.dto;
import java.math.BigDecimal;
public record ProductSummaryResponse(Long id, String name, String slug, String description, BigDecimal price,
        String imagePath, boolean available, boolean featured, CategoryResponse category) {}
