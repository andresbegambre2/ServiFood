package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "order_item_extras")
public class OrderItemExtra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_item_id", nullable = false) private OrderItem orderItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "extra_id") private Extra extra;
    @NotBlank @Size(max = 120) @Column(name = "extra_name_snapshot", nullable = false, length = 120) private String extraNameSnapshot;
    @NotNull @DecimalMin("0.00") @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2) private BigDecimal unitPriceSnapshot;
    @Positive @Column(nullable = false) private int quantity;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    protected OrderItemExtra() {}
    public OrderItemExtra(Extra extra, int quantity) { this.extra = extra; this.extraNameSnapshot = extra.getName(); this.unitPriceSnapshot = extra.getPrice(); this.quantity = quantity; this.subtotal = unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity)); }
    void assignTo(OrderItem orderItem) { this.orderItem = orderItem; }
    public String getExtraNameSnapshot() { return extraNameSnapshot; } public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public int getQuantity() { return quantity; } public BigDecimal getSubtotal() { return subtotal; }
    public Extra getExtra() { return extra; }
}
