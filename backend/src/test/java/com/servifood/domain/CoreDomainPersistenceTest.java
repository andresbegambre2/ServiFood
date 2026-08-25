package com.servifood.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class CoreDomainPersistenceTest {
    @Autowired CategoryRepository categories;
    @Autowired ExtraRepository extras;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired CustomerOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired InternalUserRepository users;
    @Autowired BusinessSettingsRepository settings;
    @Autowired BusinessHoursRepository hours;
    @Autowired PromotionRepository promotions;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void persistsCatalogWithAllowedExtras() {
        Category category = categories.save(new Category("Hamburguesas test", "hamburguesas-test", 1));
        Extra extra = extras.save(new Extra("Cheddar test", money("3000")));
        Product product = new Product("Doble test", "doble-test", "Doble carne y queso", money("25000"), category);
        product.allowExtra(extra); products.saveAndFlush(product); entityManager.clear();

        Product stored = products.findBySlug("doble-test").orElseThrow();
        assertThat(stored.getCategory().getName()).isEqualTo("Hamburguesas test");
        assertThat(stored.getAllowedExtras()).extracting(Extra::getName).containsExactly("Cheddar test");
        assertThat(stored.getCreatedAt()).isNotNull();
    }

    @Test
    void cascadesCustomerAddresses() {
        Customer customer = new Customer("Cliente Test", "3001234567", "cliente@example.com");
        customer.addAddress(new CustomerAddress("Casa", "Calle 10 # 20-30", "Centro", "Portería", true));
        Long id = customers.saveAndFlush(customer).getId(); entityManager.clear();
        Customer stored = customers.findById(id).orElseThrow();
        assertThat(stored.getAddresses()).hasSize(1);
        assertThat(stored.getAddresses().getFirst().isPrimaryAddress()).isTrue();
    }

    @Test
    void orderKeepsProductAndExtraPriceSnapshots() {
        Category category = categories.save(new Category("Combos test", "combos-test", 2));
        Extra extra = extras.save(new Extra("Tocineta test", money("3000")));
        Product product = products.save(new Product("Combo test", "combo-test", "Producto de prueba", money("20000"), category));
        Customer customer = customers.save(new Customer("Ana", "3007654321", null));
        OrderItem item = new OrderItem(product, 2, "Sin cebolla"); item.addExtra(new OrderItemExtra(extra, 2));
        CustomerOrder order = new CustomerOrder("SF-TEST-001", customer, customer.getName(), customer.getPhone(), DeliveryType.DELIVERY, "Calle 1", money("5000"));
        order.addItem(item); Long orderId = orders.saveAndFlush(order).getId();
        product.changePrice(money("99000")); products.saveAndFlush(product); entityManager.clear();

        CustomerOrder stored = orders.findById(orderId).orElseThrow();
        assertThat(stored.getSubtotal()).isEqualByComparingTo("46000.00");
        assertThat(stored.getTotal()).isEqualByComparingTo("51000.00");
        assertThat(stored.getItems().getFirst().getUnitPriceSnapshot()).isEqualByComparingTo("20000.00");
        assertThat(stored.getItems().getFirst().getExtras().getFirst().getUnitPriceSnapshot()).isEqualByComparingTo("3000.00");
    }

    @Test
    void paymentReviewIsIndependentFromOrderStatus() {
        CustomerOrder order = orders.save(new CustomerOrder("SF-TEST-002", null, "Invitado", "3001112233", DeliveryType.PICKUP, null, BigDecimal.ZERO));
        InternalUser reviewer = users.save(new InternalUser("Admin", "reviewer@example.com", passwordEncoder.encode("test-password"), UserRole.ADMIN));
        Payment payment = new Payment(order, PaymentMethod.TRANSFER, BigDecimal.ZERO); payment.approve(reviewer);
        Payment stored = payments.saveAndFlush(payment);
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(reviewer.getPasswordHash()).isNotEqualTo("test-password");
        assertThat(passwordEncoder.matches("test-password", reviewer.getPasswordHash())).isTrue();
    }

    @Test
    void persistsValidPromotionAndRejectsDuplicateCatalogSlug() {
        Promotion promotion = new Promotion("Semana Burger", DiscountType.PERCENTAGE, new BigDecimal("15"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.Instant.parse("2026-01-31T00:00:00Z"), money("30000"));
        assertThat(promotions.saveAndFlush(promotion).getId()).isNotNull();
        categories.saveAndFlush(new Category("Única A", "slug-repetido", 10));
        assertThatThrownBy(() -> categories.saveAndFlush(new Category("Única B", "slug-repetido", 11)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void persistsBusinessSettingsAndMultipleScheduleSlots() {
        settings.save(new BusinessSettings("ServiFood Test", "Demo", "3001234567", "3001234567", "Calle 1", money("5000"), 25, "COP"));
        hours.save(new BusinessHours(DayOfWeek.FRIDAY, 1, LocalTime.of(11, 0), LocalTime.of(15, 0), false));
        hours.save(new BusinessHours(DayOfWeek.FRIDAY, 2, LocalTime.of(18, 0), LocalTime.of(23, 0), false));
        entityManager.flush();
        assertThat(settings.findAll()).extracting(BusinessSettings::getCurrency).contains("COP");
        assertThat(hours.findByDayOfWeekOrderBySlotNumber(DayOfWeek.FRIDAY)).extracting(BusinessHours::getSlotNumber).containsExactly(1, 2);
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
