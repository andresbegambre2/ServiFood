package com.servifood.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "coupon_id", nullable = false) private Coupon coupon;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @NotNull @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false, unique = true) private CustomerOrder order;
    @NotNull @PositiveOrZero @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2) private BigDecimal discountAmount;
    @Column(name = "reversed_at") private Instant reversedAt;
    protected CouponRedemption() {}
    public CouponRedemption(Coupon coupon, Customer customer, CustomerOrder order, BigDecimal discountAmount) { this.coupon = coupon; this.customer = customer; this.order = order; this.discountAmount = discountAmount; }
    public void reverse() { if (reversedAt == null) reversedAt = Instant.now(); }
    public Long getId() { return id; } public Coupon getCoupon() { return coupon; } public Customer getCustomer() { return customer; }
    public CustomerOrder getOrder() { return order; } public BigDecimal getDiscountAmount() { return discountAmount; } public Instant getReversedAt() { return reversedAt; }
}
