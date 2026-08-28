package com.servifood.presentation.rest.dto;

import java.util.List;
import com.servifood.presentation.rest.dto.LoyaltyDtos.LoyaltyQuote;

public record CheckoutQuoteResponse(OrderTotalsResponse totals, List<OrderItemSnapshotResponse> items, LoyaltyQuote loyalty) {}
