package com.servifood.presentation.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KitchenOperationsApiTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwords;
    @Autowired InternalUserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ExtraRepository extras;
    @Autowired CustomerOrderRepository orders;
    @Autowired PaymentRepository payments;

    Product product;
    Extra extra;
    InternalUser admin;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        admin = users.save(new InternalUser("Admin Cocina", "admin-kitchen-" + suffix + "@servifood.local", passwords.encode("test-password"), UserRole.ADMIN));
        Category category = categories.save(new Category("Cocina " + suffix, "cocina-" + suffix, 1));
        product = products.save(new Product("Doble Smash", "doble-smash-" + suffix, "Producto para cocina", money("22000"), category));
        extra = extras.save(new Extra("Tocineta " + suffix, money("4000")));
    }

    @Test
    void protectsKitchenApiByRole() throws Exception {
        mvc.perform(get("/api/v1/kitchen/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/kitchen/orders").with(user("cashier@servifood.local").roles("CASHIER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/kitchen/orders").with(user("kitchen@servifood.local").roles("KITCHEN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/kitchen/orders").with(user("admin@servifood.local").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void listsEligibleActiveOrdersOldestFirstWithKitchenDetails() throws Exception {
        CustomerOrder oldest = order("SF-K-OLD", DeliveryType.DELIVERY, OrderStatus.CONFIRMED, PaymentMethod.CASH, false);
        CustomerOrder preparing = order("SF-K-PREP", DeliveryType.PICKUP, OrderStatus.PREPARING, PaymentMethod.PAY_ON_PICKUP, false);
        order("SF-K-NEW", DeliveryType.PICKUP, OrderStatus.NEW, PaymentMethod.CASH, false);
        order("SF-K-REVIEW", DeliveryType.DELIVERY, OrderStatus.CONFIRMED, PaymentMethod.TRANSFER, false);

        mvc.perform(get("/api/v1/kitchen/orders").with(user("kitchen@servifood.local").roles("KITCHEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].publicNumber").value(oldest.getPublicNumber()))
                .andExpect(jsonPath("$[0].stage").value("NEW"))
                .andExpect(jsonPath("$[0].deliveryType").value("DELIVERY"))
                .andExpect(jsonPath("$[0].items[0].name").value("Doble Smash"))
                .andExpect(jsonPath("$[0].items[0].notes").value("Sin cebolla"))
                .andExpect(jsonPath("$[0].items[0].extras[0].name").value(extra.getName()))
                .andExpect(jsonPath("$[1].publicNumber").value(preparing.getPublicNumber()))
                .andExpect(jsonPath("$[1].stage").value("PREPARING"));
    }

    @Test
    void allowsOnlyKitchenPreparationTransitions() throws Exception {
        CustomerOrder order = order("SF-K-FLOW", DeliveryType.PICKUP, OrderStatus.CONFIRMED, PaymentMethod.CASH, false);
        transition(order, "PREPARING", "PREPARING");
        transition(order, "READY", "READY");
        mvc.perform(patch("/api/v1/kitchen/orders/{number}/stage", order.getPublicNumber())
                .with(user("kitchen@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"PREPARING\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/kitchen/orders/{number}/stage", order.getPublicNumber())
                .with(user("kitchen@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blocksTransferUnderReviewUntilApproved() throws Exception {
        CustomerOrder order = order("SF-K-PAY", DeliveryType.DELIVERY, OrderStatus.CONFIRMED, PaymentMethod.TRANSFER, false);
        mvc.perform(patch("/api/v1/kitchen/orders/{number}/stage", order.getPublicNumber())
                .with(user("kitchen@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"PREPARING\"}"))
                .andExpect(status().isBadRequest());
        Payment payment = payments.findFirstByOrderId(order.getId()).orElseThrow();
        payment.approve(admin); payments.saveAndFlush(payment);
        transition(order, "PREPARING", "PREPARING");
    }

    private CustomerOrder order(String number, DeliveryType delivery, OrderStatus status, PaymentMethod method, boolean approved) {
        CustomerOrder order = new CustomerOrder(number, null, "Cliente Cocina", "3001234567", delivery,
                delivery == DeliveryType.DELIVERY ? "Calle 10" : null, delivery == DeliveryType.DELIVERY ? money("5000") : money("0"));
        OrderItem item = new OrderItem(product, 2, "Sin cebolla");
        item.addExtra(new OrderItemExtra(extra, 2));
        order.addItem(item);
        order = orders.saveAndFlush(order);
        if (status != OrderStatus.NEW) order.confirm();
        if (status == OrderStatus.PREPARING || status == OrderStatus.READY) order.startPreparation();
        if (status == OrderStatus.READY) order.markReady();
        order = orders.saveAndFlush(order);
        Payment payment = new Payment(order, method, order.getTotal());
        if (method == PaymentMethod.TRANSFER) payment.submitForReview("private-proof.png");
        if (approved) payment.approve(admin);
        payments.saveAndFlush(payment);
        return order;
    }

    private void transition(CustomerOrder order, String target, String expected) throws Exception {
        mvc.perform(patch("/api/v1/kitchen/orders/{number}/stage", order.getPublicNumber())
                .with(user("kitchen@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"" + target + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value(expected));
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
