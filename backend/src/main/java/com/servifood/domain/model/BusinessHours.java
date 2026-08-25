package com.servifood.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "business_hours", uniqueConstraints = @UniqueConstraint(name = "uk_business_hours_day_slot", columnNames = {"day_of_week", "slot_number"}))
public class BusinessHours extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 12) private DayOfWeek dayOfWeek;
    @Min(1) @Max(2) @Column(name = "slot_number", nullable = false) private int slotNumber = 1;
    @Column(name = "opens_at") private LocalTime opensAt;
    @Column(name = "closes_at") private LocalTime closesAt;
    @Column(nullable = false) private boolean closed;
    protected BusinessHours() {}
    public BusinessHours(DayOfWeek dayOfWeek, int slotNumber, LocalTime opensAt, LocalTime closesAt, boolean closed) { this.dayOfWeek = dayOfWeek; this.slotNumber = slotNumber; this.opensAt = opensAt; this.closesAt = closesAt; this.closed = closed; }
    @AssertTrue(message = "open schedules require an opening and closing time")
    public boolean isTimeRangeValid() { return closed ? opensAt == null && closesAt == null : opensAt != null && closesAt != null && closesAt.isAfter(opensAt); }
    public Long getId() { return id; } public DayOfWeek getDayOfWeek() { return dayOfWeek; } public int getSlotNumber() { return slotNumber; }
    public boolean isClosed() { return closed; }
    public LocalTime getOpensAt() { return opensAt; } public LocalTime getClosesAt() { return closesAt; }
}
