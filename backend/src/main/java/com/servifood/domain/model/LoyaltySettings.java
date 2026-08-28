package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "loyalty_settings")
public class LoyaltySettings extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @DecimalMin(value = "0.01") @Column(name = "amount_per_point", nullable = false, precision = 12, scale = 2) private BigDecimal amountPerPoint;
    @Positive @Column(name = "minimum_points_to_redeem", nullable = false) private int minimumPointsToRedeem;
    @Min(1) @Max(100) @Column(name = "maximum_redemption_percentage", nullable = false) private int maximumRedemptionPercentage;
    @Column(nullable = false) private boolean active;
    protected LoyaltySettings() {}
    public LoyaltySettings(BigDecimal amountPerPoint, int minimumPointsToRedeem, int maximumRedemptionPercentage, boolean active) { update(amountPerPoint, minimumPointsToRedeem, maximumRedemptionPercentage, active); }
    public void update(BigDecimal amountPerPoint, int minimumPointsToRedeem, int maximumRedemptionPercentage, boolean active) { this.amountPerPoint = amountPerPoint; this.minimumPointsToRedeem = minimumPointsToRedeem; this.maximumRedemptionPercentage = maximumRedemptionPercentage; this.active = active; }
    public Long getId() { return id; } public BigDecimal getAmountPerPoint() { return amountPerPoint; }
    public int getMinimumPointsToRedeem() { return minimumPointsToRedeem; } public int getMaximumRedemptionPercentage() { return maximumRedemptionPercentage; }
    public boolean isActive() { return active; }
}
