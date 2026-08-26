package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemSnapshotResponse(String name, BigDecimal unitPrice, int quantity, String notes,
        BigDecimal subtotal, List<OrderExtraSnapshotResponse> extras) {}
