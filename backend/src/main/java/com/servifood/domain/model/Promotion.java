package com.servifood.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "promotions")
public class Promotion extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) private String name;
    @Size(max = 1000) @Column(length = 1000) private String description;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false, length = 25) private DiscountType discountType;
    @NotNull @DecimalMin("0.00") @Column(name = "discount_value", nullable = false, precision = 12, scale = 2) private BigDecimal discountValue;
    @NotNull @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @NotNull @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @NotNull @DecimalMin("0.00") @Column(name = "minimum_purchase", nullable = false, precision = 12, scale = 2) private BigDecimal minimumPurchase = BigDecimal.ZERO;
    @Positive @Column(name = "usage_limit") private Integer usageLimit;
    @Column(nullable = false) private boolean active = true;
    protected Promotion() {}
    public Promotion(String name, DiscountType type, BigDecimal value, Instant startsAt, Instant endsAt, BigDecimal minimumPurchase) { this.name = name; this.discountType = type; this.discountValue = value; this.startsAt = startsAt; this.endsAt = endsAt; this.minimumPurchase = minimumPurchase; }
    public void setDescription(String description) { this.description = description; }
    @AssertTrue(message = "promotion end must be after start") public boolean isDateRangeValid() { return startsAt == null || endsAt == null || endsAt.isAfter(startsAt); }
    @AssertTrue(message = "percentage discount must be between 0 and 100") public boolean isDiscountValid() { return discountType != DiscountType.PERCENTAGE || (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) >= 0 && discountValue.compareTo(new BigDecimal("100")) <= 0); }
    public Long getId() { return id; } public BigDecimal getDiscountValue() { return discountValue; }
    public String getName() { return name; } public String getDescription() { return description; }
    public DiscountType getDiscountType() { return discountType; } public Instant getStartsAt() { return startsAt; } public Instant getEndsAt() { return endsAt; }
    public BigDecimal getMinimumPurchase() { return minimumPurchase; } public boolean isActive() { return active; }
    public void deactivate() { active = false; }
}
