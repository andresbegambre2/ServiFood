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
    @NotBlank @Size(max = 60) @Column(name = "time_zone", nullable = false, length = 60) private String timeZone = "America/Bogota";
    @Size(max = 120) @Column(name = "transfer_provider", length = 120) private String transferProvider;
    @Size(max = 150) @Column(name = "transfer_account_holder", length = 150) private String transferAccountHolder;
    @Size(max = 120) @Column(name = "transfer_account_reference", length = 120) private String transferAccountReference;
    @Size(max = 500) @Column(name = "payment_qr_path", length = 500) private String paymentQrPath;
    protected BusinessSettings() {}
    public BusinessSettings(String tradeName, String description, String phone, String whatsapp, String address, BigDecimal deliveryFee, int preparationMinutes, String currency) {
        this.tradeName = tradeName; this.description = description; this.phone = phone; this.whatsapp = whatsapp; this.address = address;
        this.baseDeliveryFee = deliveryFee; this.estimatedPreparationMinutes = preparationMinutes; this.currency = currency;
    }
    public void setSocialLinks(String instagram, String facebook) { this.instagram = instagram; this.facebook = facebook; }
    public void configureCheckout(String timeZone, String transferProvider, String accountHolder, String accountReference, String qrPath) {
        this.timeZone = timeZone; this.transferProvider = transferProvider; this.transferAccountHolder = accountHolder;
        this.transferAccountReference = accountReference; this.paymentQrPath = qrPath;
    }
    public void updatePublicDetails(String tradeName, String description, String phone, String whatsapp, String address, String instagram, String facebook, BigDecimal deliveryFee, int preparationMinutes) {
        this.tradeName = tradeName; this.description = description; this.phone = phone; this.whatsapp = whatsapp; this.address = address;
        this.instagram = instagram; this.facebook = facebook; this.baseDeliveryFee = deliveryFee; this.estimatedPreparationMinutes = preparationMinutes;
    }
    public Long getId() { return id; } public String getTradeName() { return tradeName; }
    public BigDecimal getBaseDeliveryFee() { return baseDeliveryFee; } public String getCurrency() { return currency; }
    public String getDescription() { return description; } public String getLogoPath() { return logoPath; }
    public String getPhone() { return phone; } public String getWhatsapp() { return whatsapp; } public String getAddress() { return address; }
    public String getInstagram() { return instagram; } public String getFacebook() { return facebook; }
    public Integer getEstimatedPreparationMinutes() { return estimatedPreparationMinutes; }
    public String getTimeZone() { return timeZone; } public String getTransferProvider() { return transferProvider; }
    public String getTransferAccountHolder() { return transferAccountHolder; } public String getTransferAccountReference() { return transferAccountReference; }
    public String getPaymentQrPath() { return paymentQrPath; }
}
