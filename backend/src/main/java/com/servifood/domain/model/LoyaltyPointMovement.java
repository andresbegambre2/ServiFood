package com.servifood.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "loyalty_point_movements")
public class LoyaltyPointMovement extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private InternalUser createdBy;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 30) private LoyaltyMovementType type;
    @Column(name = "points_delta", nullable = false) private int pointsDelta;
    @PositiveOrZero @Column(name = "balance_after", nullable = false) private int balanceAfter;
    @NotBlank @Size(max = 500) @Column(nullable = false, length = 500) private String reason;
    protected LoyaltyPointMovement() {}
    public LoyaltyPointMovement(Customer customer, CustomerOrder order, InternalUser createdBy, LoyaltyMovementType type, int pointsDelta, String reason) { this.customer = customer; this.order = order; this.createdBy = createdBy; this.type = type; this.pointsDelta = pointsDelta; this.balanceAfter = customer.getPointsBalance(); this.reason = reason.trim(); }
    public Long getId() { return id; } public Customer getCustomer() { return customer; } public CustomerOrder getOrder() { return order; }
    public InternalUser getCreatedBy() { return createdBy; } public LoyaltyMovementType getType() { return type; }
    public int getPointsDelta() { return pointsDelta; } public int getBalanceAfter() { return balanceAfter; } public String getReason() { return reason; }
}
