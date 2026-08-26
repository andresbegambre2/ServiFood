package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import com.servifood.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PaymentCheckoutRequest(@NotNull PaymentMethod method, @DecimalMin("0.00") BigDecimal cashTendered) {}
