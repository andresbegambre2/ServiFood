package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderExtraRequest(@NotNull @Positive Long extraId, @DecimalMin("0.00") BigDecimal expectedUnitPrice) {}
