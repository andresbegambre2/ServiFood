package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;

public record OrderTotalsResponse(BigDecimal subtotal, BigDecimal discount, BigDecimal deliveryFee,
        BigDecimal total, String currency, int estimatedMinutes) {}
