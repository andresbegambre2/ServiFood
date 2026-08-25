package com.servifood.presentation.rest.dto;
import java.math.BigDecimal;
public record ExtraResponse(Long id, String name, String description, BigDecimal price) {}
