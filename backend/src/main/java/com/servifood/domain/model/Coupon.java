package com.servifood.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "coupons")
public class Coupon extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Pattern(regexp = "[A-Z0-9_-]+") @Size(max = 40) @Column(nullable = false, unique = true, length = 40) private String code;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false, length = 25) private DiscountType discountType;
    @NotNull @DecimalMin(value = "0.01") @Column(name = "discount_value", nullable = false, precision = 12, scale = 2) private BigDecimal discountValue;
    @NotNull @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @NotNull @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @NotNull @DecimalMin("0.00") @Column(name = "minimum_purchase", nullable = false, precision = 12, scale = 2) private BigDecimal minimumPurchase;
    @Positive @Column(name = "total_usage_limit") private Integer totalUsageLimit;
    @Positive @Column(name = "per_customer_usage_limit") private Integer perCustomerUsageLimit;
    @Column(nullable = false) private boolean active;
    @Version private long version;
    protected Coupon() {}
    public Coupon(String code, DiscountType type, BigDecimal value, Instant startsAt, Instant endsAt, BigDecimal minimumPurchase, Integer totalLimit, Integer customerLimit, boolean active) { update(code, type, value, startsAt, endsAt, minimumPurchase, totalLimit, customerLimit, active); }
    public void update(String code, DiscountType type, BigDecimal value, Instant startsAt, Instant endsAt, BigDecimal minimumPurchase, Integer totalLimit, Integer customerLimit, boolean active) { this.code = code.trim().toUpperCase(Locale.ROOT); this.discountType = type; this.discountValue = value; this.startsAt = startsAt; this.endsAt = endsAt; this.minimumPurchase = minimumPurchase; this.totalUsageLimit = totalLimit; this.perCustomerUsageLimit = customerLimit; this.active = active; }
    @AssertTrue(message = "La fecha final debe ser posterior a la inicial") public boolean isDateRangeValid() { return startsAt == null || endsAt == null || endsAt.isAfter(startsAt); }
    @AssertTrue(message = "El porcentaje debe estar entre 0 y 100") public boolean isDiscountValid() { return discountType != DiscountType.PERCENTAGE || (discountValue != null && discountValue.signum() > 0 && discountValue.compareTo(new BigDecimal("100")) <= 0); }
    public Long getId() { return id; } public String getCode() { return code; } public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; } public Instant getStartsAt() { return startsAt; } public Instant getEndsAt() { return endsAt; }
    public BigDecimal getMinimumPurchase() { return minimumPurchase; } public Integer getTotalUsageLimit() { return totalUsageLimit; }
    public Integer getPerCustomerUsageLimit() { return perCustomerUsageLimit; } public boolean isActive() { return active; }
}
