package com.servifood.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.servifood.domain.exception.DomainException;

@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private CustomerOrder order;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25) private PaymentMethod method;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25) private PaymentStatus status = PaymentStatus.PENDING;
    @NotNull @DecimalMin("0.00") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Size(max = 500) @Column(name = "receipt_path", length = 500) private String receiptPath;
    @Size(max = 500) @Column(name = "rejection_reason", length = 500) private String rejectionReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private InternalUser reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    protected Payment() {}
    public Payment(CustomerOrder order, PaymentMethod method, BigDecimal amount) { this.order = order; this.method = method; this.amount = amount; }
    public void submitForReview(String receiptPath) { if (method != PaymentMethod.TRANSFER) throw new DomainException("only transfers can be submitted for review"); if (receiptPath == null || receiptPath.isBlank()) throw new DomainException("transfer receipt is required"); this.receiptPath = receiptPath; status = PaymentStatus.UNDER_REVIEW; }
    public void approve(InternalUser reviewer) { status = PaymentStatus.APPROVED; reviewedBy = reviewer; reviewedAt = Instant.now(); rejectionReason = null; }
    public void reject(InternalUser reviewer, String reason) { status = PaymentStatus.REJECTED; reviewedBy = reviewer; reviewedAt = Instant.now(); rejectionReason = reason; }
    public Long getId() { return id; } public PaymentStatus getStatus() { return status; } public BigDecimal getAmount() { return amount; }
    @AssertTrue(message = "rejected payments require a reason") public boolean isRejectionValid() { return status != PaymentStatus.REJECTED || (rejectionReason != null && !rejectionReason.isBlank()); }
}
