package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "business_settings")
public class BusinessSettings extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 150) @Column(name = "trade_name", nullable = false, length = 150) private String tradeName;
    @Size(max = 2000) @Column(length = 2000) private String description;
    @Size(max = 500) @Column(name = "logo_path", length = 500) private String logoPath;
    @NotBlank @Size(min = 7, max = 30) @Column(nullable = false, length = 30) private String phone;
    @NotBlank @Size(min = 7, max = 30) @Column(nullable = false, length = 30) private String whatsapp;
    @NotBlank @Size(max = 300) @Column(nullable = false, length = 300) private String address;
    @Size(max = 200) @Column(length = 200) private String instagram;
    @Size(max = 200) @Column(length = 200) private String facebook;
    @NotNull @DecimalMin("0.00") @Column(name = "base_delivery_fee", nullable = false, precision = 12, scale = 2) private BigDecimal baseDeliveryFee;
    @NotNull @Positive @Column(name = "estimated_preparation_minutes", nullable = false) private Integer estimatedPreparationMinutes;
    @NotBlank @Pattern(regexp = "[A-Z]{3}") @Column(nullable = false, length = 3) private String currency;
    protected BusinessSettings() {}
    public BusinessSettings(String tradeName, String description, String phone, String whatsapp, String address, BigDecimal deliveryFee, int preparationMinutes, String currency) {
        this.tradeName = tradeName; this.description = description; this.phone = phone; this.whatsapp = whatsapp; this.address = address;
        this.baseDeliveryFee = deliveryFee; this.estimatedPreparationMinutes = preparationMinutes; this.currency = currency;
    }
    public Long getId() { return id; } public String getTradeName() { return tradeName; }
    public BigDecimal getBaseDeliveryFee() { return baseDeliveryFee; } public String getCurrency() { return currency; }
}
