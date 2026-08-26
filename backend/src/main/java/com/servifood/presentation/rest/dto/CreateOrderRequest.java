package com.servifood.presentation.rest.dto;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull UUID clientRequestId,
        @NotNull @Valid CustomerCheckoutRequest customer,
        @NotNull @Valid DeliveryCheckoutRequest delivery,
        @NotNull @Valid PaymentCheckoutRequest payment,
        @NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> lines) {}
