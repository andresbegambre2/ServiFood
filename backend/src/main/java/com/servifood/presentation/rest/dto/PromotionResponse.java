package com.servifood.presentation.rest.dto;
import java.math.BigDecimal;
import java.time.Instant;
import com.servifood.domain.model.DiscountType;
public record PromotionResponse(Long id, String name, String description, DiscountType discountType,
        BigDecimal discountValue, Instant startsAt, Instant endsAt, BigDecimal minimumPurchase) {}
