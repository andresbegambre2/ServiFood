package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.*;
import com.servifood.domain.model.*;

public final class LoyaltyDtos {
    private LoyaltyDtos() {}
    public record LoyaltyQuote(boolean active, int availablePoints, int pointsRedeemed, BigDecimal pointsDiscount,
            String couponCode, BigDecimal couponDiscount, int pointsToEarn, int minimumPointsToRedeem,
            int maximumRedemptionPercentage, BigDecimal amountPerPoint) {}
    public record LoyaltySettingsView(Long id, BigDecimal amountPerPoint, int minimumPointsToRedeem, int maximumRedemptionPercentage, boolean active) {}
    public record LoyaltySettingsRequest(@NotNull @DecimalMin("0.01") BigDecimal amountPerPoint,
            @Positive int minimumPointsToRedeem, @Min(1) @Max(100) int maximumRedemptionPercentage, boolean active) {}
    public record CouponView(Long id, String code, DiscountType discountType, BigDecimal discountValue, Instant startsAt,
            Instant endsAt, BigDecimal minimumPurchase, Integer totalUsageLimit, Integer perCustomerUsageLimit,
            long uses, boolean active) {}
    public record CouponRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]+") @Size(max = 40) String code,
            @NotNull DiscountType discountType, @NotNull @DecimalMin("0.01") BigDecimal discountValue,
            @NotNull Instant startsAt, @NotNull Instant endsAt, @NotNull @DecimalMin("0.00") BigDecimal minimumPurchase,
            @Positive Integer totalUsageLimit, @Positive Integer perCustomerUsageLimit, boolean active) {}
    public record CustomerSummary(Long id, String name, String phone, int points, long orderCount, BigDecimal totalSpent, Instant lastOrderAt) {}
    public record AddressView(Long id, String label, String address, String neighborhood, String reference, boolean primary) {}
    public record CustomerOrderView(String publicNumber, Instant createdAt, OrderStatus status, BigDecimal total,
            BigDecimal discount, String couponCode, int pointsRedeemed, int pointsEarned) {}
    public record FrequentProduct(Long productId, String name, long quantity) {}
    public record PointMovementView(Long id, LoyaltyMovementType type, int pointsDelta, int balanceAfter, String reason,
            String orderNumber, String createdBy, Instant createdAt) {}
    public record CustomerProfile(Long id, String name, String phone, String email, int points, long orderCount,
            BigDecimal totalSpent, Instant lastOrderAt, List<AddressView> addresses, List<CustomerOrderView> orders,
            List<FrequentProduct> frequentProducts, List<PointMovementView> pointMovements) {}
    public record PointAdjustment(@NotNull @Min(-1000000) @Max(1000000) Integer points,
            @NotBlank @Size(max = 500) String reason) { @AssertTrue(message = "El ajuste no puede ser cero") public boolean isNonZero() { return points == null || points != 0; } }
    public record RepeatExtra(Long id, String name, BigDecimal price) {}
    public record RepeatLine(Long productId, String slug, String name, String imagePath, BigDecimal price, int quantity,
            String notes, List<RepeatExtra> extras) {}
    public record RepeatOrderResponse(List<RepeatLine> lines) {}
}
