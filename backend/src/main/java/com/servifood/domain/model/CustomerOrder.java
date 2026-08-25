package com.servifood.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "orders")
public class CustomerOrder extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 30) @Column(name = "public_number", nullable = false, unique = true, length = 30) private String publicNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @NotBlank @Size(max = 120) @Column(name = "customer_name_snapshot", nullable = false, length = 120) private String customerNameSnapshot;
    @NotBlank @Size(min = 7, max = 30) @Column(name = "customer_phone_snapshot", nullable = false, length = 30) private String customerPhoneSnapshot;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "delivery_type", nullable = false, length = 20) private DeliveryType deliveryType;
    @Size(max = 500) @Column(name = "delivery_address_snapshot", length = 500) private String deliveryAddressSnapshot;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2) private BigDecimal deliveryFee = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discount = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Size(max = 1000) @Column(length = 1000) private String notes;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status = OrderStatus.NEW;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "prepared_at") private Instant preparedAt;
    @Column(name = "ready_at") private Instant readyAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Valid @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) private List<OrderItem> items = new ArrayList<>();
    protected CustomerOrder() {}
    public CustomerOrder(String publicNumber, Customer customer, String customerName, String customerPhone, DeliveryType deliveryType, String deliveryAddress, BigDecimal deliveryFee) {
        this.publicNumber = publicNumber; this.customer = customer; this.customerNameSnapshot = customerName; this.customerPhoneSnapshot = customerPhone;
        this.deliveryType = deliveryType; this.deliveryAddressSnapshot = deliveryAddress; this.deliveryFee = deliveryFee; recalculate();
    }
    public void addItem(OrderItem item) { item.assignTo(this); items.add(item); recalculate(); }
    public void applyDiscount(BigDecimal discount) { this.discount = discount; recalculate(); }
    private void recalculate() {
        subtotal = items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(deliveryFee == null ? BigDecimal.ZERO : deliveryFee).subtract(discount == null ? BigDecimal.ZERO : discount).max(BigDecimal.ZERO);
    }
    @AssertTrue(message = "delivery address is required for delivery orders")
    public boolean isDeliveryAddressValid() { return deliveryType != DeliveryType.DELIVERY || (deliveryAddressSnapshot != null && !deliveryAddressSnapshot.isBlank()); }
    @AssertTrue(message = "order totals are inconsistent")
    public boolean isFinancialBreakdownValid() { return subtotal != null && deliveryFee != null && discount != null && total != null && total.signum() >= 0 && total.compareTo(subtotal.add(deliveryFee).subtract(discount).max(BigDecimal.ZERO)) == 0; }
    public Long getId() { return id; } public String getPublicNumber() { return publicNumber; } public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; } public BigDecimal getTotal() { return total; } public List<OrderItem> getItems() { return List.copyOf(items); }
    public String getCustomerNameSnapshot() { return customerNameSnapshot; }
}
