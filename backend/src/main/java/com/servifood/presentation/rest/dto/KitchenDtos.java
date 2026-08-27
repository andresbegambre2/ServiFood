package com.servifood.presentation.rest.dto;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import com.servifood.domain.model.DeliveryType;

public final class KitchenDtos {
    private KitchenDtos() {}

    public enum KitchenStage { NEW, PREPARING, READY }

    public record KitchenOrder(String publicNumber, Instant createdAt, KitchenStage stage,
            DeliveryType deliveryType, String notes, List<KitchenItem> items) {}

    public record KitchenItem(String name, int quantity, String notes, List<KitchenExtra> extras) {}

    public record KitchenExtra(String name, int quantity) {}

    public record KitchenStageChange(@NotNull KitchenStage target) {}
}
