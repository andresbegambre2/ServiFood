package com.servifood.presentation.rest.dto;
import java.math.BigDecimal;
import java.util.List;
public record BusinessPublicResponse(String tradeName, String description, String logoPath, String phone,
        String whatsapp, String address, String instagram, String facebook, BigDecimal baseDeliveryFee,
        Integer estimatedPreparationMinutes, String currency, List<BusinessHoursResponse> hours) {}
