package com.servifood.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import jakarta.validation.*;
import com.servifood.domain.model.*;

class CoreDomainValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNegativeProductPriceAndInvalidCustomerEmail() {
        Category category = new Category("Test", "test", 0);
        Product product = new Product("Producto", "producto", "Descripción", new BigDecimal("-1.00"), category);
        Customer customer = new Customer("Cliente", "3001234567", "correo-invalido");
        assertThat(validator.validate(product)).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
        assertThat(validator.validate(customer)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validatesPromotionDatesAndPercentageRange() {
        Instant start = Instant.parse("2026-01-02T00:00:00Z");
        Promotion promotion = new Promotion("Inválida", DiscountType.PERCENTAGE, new BigDecimal("101"), start, start.minusSeconds(1), BigDecimal.ZERO);
        Set<ConstraintViolation<Promotion>> violations = validator.validate(promotion);
        assertThat(violations).extracting(ConstraintViolation::getMessage).contains("promotion end must be after start", "percentage discount must be between 0 and 100");
    }

    @Test
    void validatesDeliveryAddressAndBusinessHours() {
        CustomerOrder delivery = new CustomerOrder("SF-INVALID", null, "Cliente", "3001234567", DeliveryType.DELIVERY, null, BigDecimal.ZERO);
        BusinessHours hours = new BusinessHours(DayOfWeek.MONDAY, 1, LocalTime.of(18, 0), LocalTime.of(12, 0), false);
        assertThat(validator.validate(delivery)).anyMatch(v -> v.getMessage().contains("delivery address"));
        assertThat(validator.validate(hours)).anyMatch(v -> v.getMessage().contains("opening and closing"));
    }

    @Test
    void exposesStableEnums() {
        assertThat(UserRole.valueOf("KITCHEN")).isEqualTo(UserRole.KITCHEN);
        assertThat(OrderStatus.values()).contains(OrderStatus.NEW, OrderStatus.ON_THE_WAY, OrderStatus.CANCELLED);
        assertThat(PaymentMethod.values()).containsExactly(PaymentMethod.CASH, PaymentMethod.TRANSFER, PaymentMethod.PAY_ON_PICKUP);
    }

    @Test
    void enforcesOrderStateTransitions() {
        CustomerOrder order = new CustomerOrder("SF-STATES", null, "Cliente", "3001234567", DeliveryType.PICKUP, null, BigDecimal.ZERO);
        order.confirm(); order.startPreparation(); order.markReady(); order.deliver();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getConfirmedAt()).isNotNull(); assertThat(order.getPreparedAt()).isNotNull();
        assertThat(order.getReadyAt()).isNotNull(); assertThat(order.getDeliveredAt()).isNotNull();
        assertThatThrownBy(order::cancel).isInstanceOf(com.servifood.domain.exception.DomainException.class);
    }
}
