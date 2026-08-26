package com.servifood.presentation.rest.dto;

import java.time.Instant;
import java.util.List;
import com.servifood.domain.model.*;

public record OrderCreatedResponse(String publicNumber, String trackingToken, OrderStatus status,
        PaymentMethod paymentMethod, PaymentStatus paymentStatus, DeliveryType deliveryType,
        String deliveryAddress, String customerName, Instant createdAt, OrderTotalsResponse totals,
        List<OrderItemSnapshotResponse> items, String businessWhatsapp, boolean idempotent) {}
