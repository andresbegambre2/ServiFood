package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OrderLineRequest(
        @NotNull @Positive Long productId,
        @Positive @Max(99) int quantity,
        @Size(max = 240) String notes,
        @DecimalMin("0.00") BigDecimal expectedUnitPrice,
        @NotNull @Size(max = 20) List<@Valid OrderExtraRequest> extras) {}
