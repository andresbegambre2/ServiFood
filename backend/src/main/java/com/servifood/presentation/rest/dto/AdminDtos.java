package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.servifood.domain.model.*;

public final class AdminDtos {
    private AdminDtos() {}

    public record Dashboard(BigDecimal salesToday, long ordersToday, long newOrders, long preparingOrders,
            long paymentsUnderReview, BigDecimal averageTicket, List<OrderSummary> latestOrders, List<TopProduct> topProducts) {}
    public record TopProduct(String name, long quantity) {}
    public record OrderSummary(String publicNumber, String customerName, Instant createdAt, DeliveryType deliveryType,
            BigDecimal total, PaymentMethod paymentMethod, PaymentStatus paymentStatus, OrderStatus orderStatus) {}
    public record OrderDetail(String publicNumber, String customerName, String customerPhone, String customerEmail,
            String deliveryAddress, DeliveryType deliveryType, BigDecimal subtotal, BigDecimal discount, BigDecimal deliveryFee,
            BigDecimal total, OrderStatus status, Instant createdAt, List<OrderItemView> items, PaymentView payment, OrderTimeline timeline) {}
    public record OrderItemView(String name, BigDecimal unitPrice, int quantity, String notes, BigDecimal subtotal, List<OrderExtraView> extras) {}
    public record OrderExtraView(String name, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {}
    public record PaymentView(Long id, PaymentMethod method, PaymentStatus status, BigDecimal amount, BigDecimal cashTendered,
            boolean receiptAvailable, String reviewerName, Instant reviewedAt, String rejectionReason) {}
    public record PaymentQueueItem(String publicNumber, String customerName, Instant createdAt, BigDecimal amount,
            PaymentMethod method, PaymentStatus status, boolean receiptAvailable) {}
    public record OrderTimeline(Instant createdAt, Instant confirmedAt, Instant preparedAt, Instant readyAt,
            Instant onTheWayAt, Instant deliveredAt, Instant cancelledAt, String cancellationReason) {}
    public record OrderFilters(OrderStatus status, PaymentMethod paymentMethod, DeliveryType deliveryType, String date, String query) {}
    public record StatusChange(@NotNull OrderStatus status, @Size(max = 500) String reason) {}
    public record RejectPayment(@NotBlank @Size(max = 500) String reason) {}

    public record ProductView(Long id, String name, String slug, String description, BigDecimal price, String imagePath,
            boolean available, boolean featured, Long categoryId, String categoryName, Set<Long> extraIds) {}
    public record ProductRequest(@NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 170) String slug,
            @NotBlank @Size(max = 2000) String description, @NotNull @DecimalMin("0.00") BigDecimal price,
            @NotNull Long categoryId, boolean available, boolean featured, Set<Long> extraIds) {}
    public record CategoryView(Long id, String name, String slug, String description, int displayOrder, boolean active) {}
    public record CategoryRequest(@NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 120) String slug,
            @Size(max = 500) String description, @PositiveOrZero int displayOrder, boolean active) {}
    public record PromotionView(Long id, String name, String description, DiscountType discountType, BigDecimal discountValue,
            Instant startsAt, Instant endsAt, BigDecimal minimumPurchase, Integer usageLimit, boolean active) {}
    public record PromotionRequest(@NotBlank @Size(max = 150) String name, @Size(max = 1000) String description,
            @NotNull DiscountType discountType, @NotNull @DecimalMin("0.00") BigDecimal discountValue,
            @NotNull Instant startsAt, @NotNull Instant endsAt, @NotNull @DecimalMin("0.00") BigDecimal minimumPurchase,
            @Positive Integer usageLimit, boolean active) {}
    public record ExtraView(Long id, String name, BigDecimal price, boolean available) {}
    public record ActiveRequest(boolean active) {}

    public record SettingsView(String tradeName, String description, String phone, String whatsapp, String address,
            String instagram, String facebook, BigDecimal baseDeliveryFee, int estimatedPreparationMinutes, String timeZone,
            String transferProvider, String transferAccountHolder, String transferAccountReference, String paymentQrPath,
            List<HoursView> hours) {}
    public record SettingsRequest(@NotBlank @Size(max = 150) String tradeName, @Size(max = 2000) String description,
            @NotBlank @Size(min = 7, max = 30) String phone, @NotBlank @Size(min = 7, max = 30) String whatsapp,
            @NotBlank @Size(max = 300) String address, @Size(max = 200) String instagram, @Size(max = 200) String facebook,
            @NotNull @DecimalMin("0.00") BigDecimal baseDeliveryFee, @Min(1) int estimatedPreparationMinutes,
            @NotBlank @Size(max = 60) String timeZone, @Size(max = 120) String transferProvider,
            @Size(max = 150) String transferAccountHolder, @Size(max = 120) String transferAccountReference,
            List<@Valid HoursRequest> hours) {}
    public record HoursView(Long id, String dayOfWeek, int slotNumber, LocalTime opensAt, LocalTime closesAt, boolean closed) {}
    public record HoursRequest(@NotBlank String dayOfWeek, @Min(1) @Max(2) int slotNumber,
            LocalTime opensAt, LocalTime closesAt, boolean closed) {
        @AssertTrue(message = "Los horarios abiertos requieren hora inicial y final válidas")
        public boolean isValidRange() { return closed ? opensAt == null && closesAt == null : opensAt != null && closesAt != null && closesAt.isAfter(opensAt); }
    }
}
