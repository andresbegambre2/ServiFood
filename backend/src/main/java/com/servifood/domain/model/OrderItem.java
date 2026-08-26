package com.servifood.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @NotBlank @Size(max = 150) @Column(name = "product_name_snapshot", nullable = false, length = 150) private String productNameSnapshot;
    @NotNull @DecimalMin("0.00") @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2) private BigDecimal unitPriceSnapshot;
    @Positive @Column(nullable = false) private int quantity;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Size(max = 500) @Column(length = 500) private String notes;
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true) private List<@Valid OrderItemExtra> extras = new ArrayList<>();
    protected OrderItem() {}
    public OrderItem(Product product, int quantity, String notes) {
        this.product = product; this.productNameSnapshot = product.getName(); this.unitPriceSnapshot = product.getPrice(); this.quantity = quantity; this.notes = notes; recalculate();
    }
    void assignTo(CustomerOrder order) { this.order = order; }
    public void addExtra(OrderItemExtra extra) { extra.assignTo(this); extras.add(extra); recalculate(); if (order != null) order.recalculate(); }
    private void recalculate() { BigDecimal extrasTotal = extras.stream().map(OrderItemExtra::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add); subtotal = unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity)).add(extrasTotal); }
    public Long getId() { return id; } public String getProductNameSnapshot() { return productNameSnapshot; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; } public int getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; } public List<OrderItemExtra> getExtras() { return List.copyOf(extras); }
    public String getNotes() { return notes; }
}
