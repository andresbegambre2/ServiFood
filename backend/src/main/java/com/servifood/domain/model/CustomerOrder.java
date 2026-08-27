package com.servifood.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.servifood.domain.exception.DomainException;

@Entity
@Table(name = "orders")
public class CustomerOrder extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 30) @Column(name = "public_number", nullable = false, unique = true, length = 30) private String publicNumber;
    @Size(max = 36) @Column(name = "client_request_id", unique = true, length = 36) private String clientRequestId;
    @Size(max = 64) @Column(name = "tracking_token_hash", length = 64) private String trackingTokenHash;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @NotBlank @Size(max = 120) @Column(name = "customer_name_snapshot", nullable = false, length = 120) private String customerNameSnapshot;
    @NotBlank @Size(min = 7, max = 30) @Column(name = "customer_phone_snapshot", nullable = false, length = 30) private String customerPhoneSnapshot;
    @Email @Size(max = 190) @Column(name = "customer_email_snapshot", length = 190) private String customerEmailSnapshot;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "delivery_type", nullable = false, length = 20) private DeliveryType deliveryType;
    @Size(max = 500) @Column(name = "delivery_address_snapshot", length = 500) private String deliveryAddressSnapshot;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2) private BigDecimal deliveryFee = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discount = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Size(max = 1000) @Column(length = 1000) private String notes;
    @Positive @Column(name = "estimated_minutes") private Integer estimatedMinutes;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status = OrderStatus.NEW;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "prepared_at") private Instant preparedAt;
    @Column(name = "ready_at") private Instant readyAt;
    @Column(name = "on_the_way_at") private Instant onTheWayAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "inventory_consumed_at") private Instant inventoryConsumedAt;
    @Column(name = "inventory_reverted_at") private Instant inventoryRevertedAt;
    @Size(max = 500) @Column(name = "cancellation_reason", length = 500) private String cancellationReason;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) private List<@Valid OrderItem> items = new ArrayList<>();
    protected CustomerOrder() {}
    public CustomerOrder(String publicNumber, Customer customer, String customerName, String customerPhone, DeliveryType deliveryType, String deliveryAddress, BigDecimal deliveryFee) {
        this.publicNumber = publicNumber; this.customer = customer; this.customerNameSnapshot = customerName; this.customerPhoneSnapshot = customerPhone;
        this.deliveryType = deliveryType; this.deliveryAddressSnapshot = deliveryAddress; this.deliveryFee = deliveryFee; recalculate();
    }
    public void configurePublicCheckout(String clientRequestId, String trackingTokenHash, String customerEmail, int estimatedMinutes) {
        this.clientRequestId = clientRequestId; this.trackingTokenHash = trackingTokenHash;
        this.customerEmailSnapshot = customerEmail; this.estimatedMinutes = estimatedMinutes;
    }
    public void addItem(OrderItem item) { item.assignTo(this); items.add(item); recalculate(); }
    public void applyDiscount(BigDecimal discount) { this.discount = discount; recalculate(); }
    void recalculate() {
        subtotal = items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(deliveryFee == null ? BigDecimal.ZERO : deliveryFee).subtract(discount == null ? BigDecimal.ZERO : discount).max(BigDecimal.ZERO);
    }
    public void confirm() { requireStatus(OrderStatus.NEW); status = OrderStatus.CONFIRMED; confirmedAt = Instant.now(); }
    public void startPreparation() { requireStatus(OrderStatus.CONFIRMED); status = OrderStatus.PREPARING; preparedAt = Instant.now(); }
    public void markReady() { requireStatus(OrderStatus.PREPARING); status = OrderStatus.READY; readyAt = Instant.now(); }
    public void markOnTheWay() { requireStatus(OrderStatus.READY); if (deliveryType != DeliveryType.DELIVERY) throw new DomainException("pickup orders cannot be marked on the way"); status = OrderStatus.ON_THE_WAY; onTheWayAt = Instant.now(); }
    public void deliver() {
        boolean validPickup = deliveryType == DeliveryType.PICKUP && status == OrderStatus.READY;
        boolean validDelivery = deliveryType == DeliveryType.DELIVERY && status == OrderStatus.ON_THE_WAY;
        if (!validPickup && !validDelivery) throw new DomainException("order cannot be delivered from status " + status);
        status = OrderStatus.DELIVERED; deliveredAt = Instant.now();
    }
    public void cancel(String reason) { if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) throw new DomainException("completed orders cannot be cancelled"); if (reason == null || reason.isBlank()) throw new DomainException("cancellation reason is required"); status = OrderStatus.CANCELLED; cancelledAt = Instant.now(); cancellationReason = reason.trim(); }
    public void cancel() { cancel("Cancelado por el restaurante"); }
    public void markInventoryConsumed() { if (inventoryConsumedAt == null) inventoryConsumedAt = Instant.now(); }
    public void markInventoryReverted() { if (inventoryConsumedAt != null && inventoryRevertedAt == null) inventoryRevertedAt = Instant.now(); }
    private void requireStatus(OrderStatus expected) { if (status != expected) throw new DomainException("expected order status " + expected + " but was " + status); }
    @AssertTrue(message = "delivery address is required for delivery orders")
    public boolean isDeliveryAddressValid() { return deliveryType != DeliveryType.DELIVERY || (deliveryAddressSnapshot != null && !deliveryAddressSnapshot.isBlank()); }
    @AssertTrue(message = "order totals are inconsistent")
    public boolean isFinancialBreakdownValid() { return subtotal != null && deliveryFee != null && discount != null && total != null && total.signum() >= 0 && total.compareTo(subtotal.add(deliveryFee).subtract(discount).max(BigDecimal.ZERO)) == 0; }
    public Long getId() { return id; } public String getPublicNumber() { return publicNumber; } public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; } public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getDiscount() { return discount; } public BigDecimal getTotal() { return total; } public List<OrderItem> getItems() { return List.copyOf(items); }
    public String getCustomerNameSnapshot() { return customerNameSnapshot; } public String getCustomerPhoneSnapshot() { return customerPhoneSnapshot; }
    public String getCustomerEmailSnapshot() { return customerEmailSnapshot; } public DeliveryType getDeliveryType() { return deliveryType; }
    public String getDeliveryAddressSnapshot() { return deliveryAddressSnapshot; } public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public String getClientRequestId() { return clientRequestId; } public String getTrackingTokenHash() { return trackingTokenHash; }
    public Instant getConfirmedAt() { return confirmedAt; } public Instant getPreparedAt() { return preparedAt; }
    public Instant getReadyAt() { return readyAt; } public Instant getDeliveredAt() { return deliveredAt; } public Instant getCancelledAt() { return cancelledAt; }
    public Instant getOnTheWayAt() { return onTheWayAt; } public String getCancellationReason() { return cancellationReason; }
    public Instant getInventoryConsumedAt() { return inventoryConsumedAt; } public Instant getInventoryRevertedAt() { return inventoryRevertedAt; }
}
