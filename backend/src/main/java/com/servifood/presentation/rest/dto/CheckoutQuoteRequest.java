package com.servifood.presentation.rest.dto;

import java.util.List;
import com.servifood.domain.model.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutQuoteRequest(@NotNull DeliveryType deliveryType,
        @NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> lines) {}
