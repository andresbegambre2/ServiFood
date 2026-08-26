package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;

public record OrderExtraSnapshotResponse(String name, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {}
