package com.servifood.presentation.rest.dto;

import java.util.List;

public record CheckoutQuoteResponse(OrderTotalsResponse totals, List<OrderItemSnapshotResponse> items) {}
