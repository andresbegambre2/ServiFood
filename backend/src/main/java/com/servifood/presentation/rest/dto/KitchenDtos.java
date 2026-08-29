package com.servifood.presentation.rest.dto;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import com.servifood.domain.model.DeliveryType;
import com.servifood.domain.model.OrderStatus;

public final class KitchenDtos {
    private KitchenDtos() {}

    public record KitchenOrder(String publicNumber, Instant createdAt, DeliveryType deliveryType,
            OrderStatus status, List<KitchenItem> items) {}
    public record KitchenItem(String name, int quantity, String notes, List<String> extras) {}
    public record KitchenStatusChange(@NotNull OrderStatus status) {}
}
