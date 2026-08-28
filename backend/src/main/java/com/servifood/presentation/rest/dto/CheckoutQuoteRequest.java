package com.servifood.presentation.rest.dto;

import java.util.List;
import com.servifood.domain.model.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record CheckoutQuoteRequest(@NotNull DeliveryType deliveryType,
        @NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> lines,
        @Size(min = 7, max = 30) String customerPhone,
        @Size(max = 40) String couponCode,
        @Min(0) Integer pointsToRedeem) {}
