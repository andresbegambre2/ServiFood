package com.servifood.presentation.rest.dto;

import com.servifood.domain.model.DeliveryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeliveryCheckoutRequest(
        @NotNull DeliveryType type,
        @Size(max = 250) String address,
        @Size(max = 120) String neighborhood,
        @Size(max = 500) String reference) {}
