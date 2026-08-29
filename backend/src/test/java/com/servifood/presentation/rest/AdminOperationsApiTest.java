package com.servifood.presentation.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import com.servifood.application.ReceiptStorage;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOperationsApiTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired PasswordEncoder passwords;
    @Autowired InternalUserRepository users; @Autowired CategoryRepository categories; @Autowired ExtraRepository extras;
    @Autowired ProductRepository products; @Autowired CustomerOrderRepository orders; @Autowired PaymentRepository payments;
    @Autowired PromotionRepository promotions; @Autowired BusinessSettingsRepository settings; @Autowired BusinessHoursRepository hours;
    @Autowired ReceiptStorage receiptStorage;
    InternalUser admin; Product product; CustomerOrder order; Payment payment;

    @BeforeEach
    void setUp() {
        admin = users.save(new InternalUser("Admin Ops", "admin-ops@servifood.local", passwords.encode("test-password"), UserRole.ADMIN));
        users.save(new InternalUser("Caja Ops", "cashier-ops@servifood.local", passwords.encode("test-password"), UserRole.CASHIER));
        users.save(new InternalUser("Cocina Ops", "kitchen-ops@servifood.local", passwords.encode("test-password"), UserRole.KITCHEN));
        Category category = categories.save(new Category("Admin test " + UUID.randomUUID(), "admin-" + UUID.randomUUID(), 1));
        product = products.save(new Product("Burger admin", "burger-" + UUID.randomUUID(), "Producto administrativo", money("20000"), category));
        OrderItem item = new OrderItem(product, 2, "Sin cebolla");
        order = new CustomerOrder("SF-ADMIN-" + UUID.randomUUID().toString().substring(0, 8), null, "Cliente Admin", "3001234567", DeliveryType.DELIVERY, "Calle 1 · Centro", money("5000"));
        order.addItem(item); order = orders.save(order);
        String receipt = receiptStorage.store(new MockMultipartFile("receipt", "proof.png", "image/png", new byte[] {(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,1,2,3,4}));
        payment = new Payment(order, PaymentMethod.TRANSFER, order.getTotal()); payment.submitForReview(receipt); payments.save(payment);
        if (settings.count() == 0) settings.save(new BusinessSettings("Distrito Test", "Demo", "3001234567", "3001234567", "Calle 1", money("5000"), 25, "COP"));
    }

    @Test
    void enforcesAdministrativeRoleMatrix() throws Exception {
        mvc.perform(get("/api/v1/admin/dashboard").with(user("admin-ops@servifood.local").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/orders").with(user("cashier-ops@servifood.local").roles("CASHIER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/products").with(user("cashier-ops@servifood.local").roles("CASHIER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/settings").with(user("cashier-ops@servifood.local").roles("CASHIER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/dashboard").with(user("kitchen-ops@servifood.local").roles("KITCHEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/kitchen/orders").with(user("cashier-ops@servifood.local").roles("CASHIER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void givesKitchenOnlyThePreparationWorkflow() throws Exception {
        mvc.perform(get("/api/v1/admin/kitchen/orders").with(user("kitchen-ops@servifood.local").roles("KITCHEN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].publicNumber").value(order.getPublicNumber()))
                .andExpect(jsonPath("$[0].customerName").doesNotExist()).andExpect(jsonPath("$[0].items[0].notes").value("Sin cebolla"));
        mvc.perform(patch("/api/v1/admin/kitchen/orders/{number}/status", order.getPublicNumber())
                .with(user("kitchen-ops@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("status", "PREPARING"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PREPARING"));
        mvc.perform(patch("/api/v1/admin/kitchen/orders/{number}/status", order.getPublicNumber())
                .with(user("kitchen-ops@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("status", "READY"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY"));
        mvc.perform(patch("/api/v1/admin/kitchen/orders/{number}/status", order.getPublicNumber())
                .with(user("kitchen-ops@servifood.local").roles("KITCHEN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("status", "DELIVERED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsDetailsAndAppliesControlledDeliveryTransitions() throws Exception {
        mvc.perform(get("/api/v1/admin/orders").param("query", "Cliente Admin").with(user("cashier-ops@servifood.local").roles("CASHIER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].publicNumber").value(order.getPublicNumber()));
        mvc.perform(get("/api/v1/admin/orders/{number}", order.getPublicNumber()).with(user("cashier-ops@servifood.local").roles("CASHIER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2));
        transition(OrderStatus.CONFIRMED, null); transition(OrderStatus.PREPARING, null); transition(OrderStatus.READY, null); transition(OrderStatus.ON_THE_WAY, null); transition(OrderStatus.DELIVERED, null);
        mvc.perform(patch("/api/v1/admin/orders/{number}/status", order.getPublicNumber()).with(user("cashier-ops@servifood.local").roles("CASHIER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelsWithReasonAndReviewsPrivateTransferReceipt() throws Exception {
        mvc.perform(get("/api/v1/admin/orders/{number}/payment/receipt", order.getPublicNumber()).with(user("cashier-ops@servifood.local").roles("CASHIER")))
                .andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options", "nosniff")).andExpect(content().contentType("image/png"));
        mvc.perform(post("/api/v1/admin/orders/{number}/payment/reject", order.getPublicNumber()).with(user("cashier-ops@servifood.local").roles("CASHIER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("reason", "Comprobante ilegible"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payment.status").value("REJECTED")).andExpect(jsonPath("$.payment.reviewerName").value("Caja Ops"));
        mvc.perform(patch("/api/v1/admin/orders/{number}/status", order.getPublicNumber()).with(user("cashier-ops@servifood.local").roles("CASHIER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("status", "CANCELLED", "reason", "Cliente desistió"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeline.cancellationReason").value("Cliente desistió"));
    }

    @Test
    void administersCatalogWithoutPhysicalDeletion() throws Exception {
        Category category = categories.findAll().getFirst();
        Map<String,Object> categoryBody = Map.of("name", "Bebidas admin", "slug", "bebidas-admin", "description", "Frías", "displayOrder", 9, "active", true);
        mvc.perform(post("/api/v1/admin/categories").with(user("admin-ops@servifood.local").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(categoryBody)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("bebidas-admin"));
        Map<String,Object> productBody = new HashMap<>(); productBody.put("name", "Nuevo producto"); productBody.put("slug", "nuevo-producto"); productBody.put("description", "Descripción completa"); productBody.put("price", 15000); productBody.put("categoryId", category.getId()); productBody.put("available", true); productBody.put("featured", false); productBody.put("extraIds", List.of());
        mvc.perform(post("/api/v1/admin/products").with(user("admin-ops@servifood.local").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(productBody)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.available").value(true));
        mvc.perform(patch("/api/v1/admin/products/{id}/active", product.getId()).with(user("admin-ops@servifood.local").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        mvc.perform(post("/api/v1/admin/products").with(user("cashier-ops@servifood.local").roles("CASHIER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(productBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administersPromotionsAndBusinessSettings() throws Exception {
        Map<String,Object> promotion = Map.of("name", "Promo admin", "description", "Temporal", "discountType", "PERCENTAGE", "discountValue", 10,
                "startsAt", Instant.now().minusSeconds(60).toString(), "endsAt", Instant.now().plusSeconds(3600).toString(), "minimumPurchase", 10000, "usageLimit", 20, "active", true);
        mvc.perform(post("/api/v1/admin/promotions").with(user("admin-ops@servifood.local").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(promotion)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usageLimit").value(20));
        Map<String,Object> schedule = Map.of("dayOfWeek", "TUESDAY", "slotNumber", 1, "opensAt", "11:00:00", "closesAt", "22:00:00", "closed", false);
        Map<String,Object> body = new HashMap<>(); body.put("tradeName", "Distrito actualizado"); body.put("description", "Operación"); body.put("phone", "3001234567"); body.put("whatsapp", "3001234567"); body.put("address", "Calle 2"); body.put("baseDeliveryFee", 6000); body.put("estimatedPreparationMinutes", 30); body.put("timeZone", "America/Bogota"); body.put("transferProvider", "Nequi"); body.put("transferAccountHolder", "Distrito"); body.put("transferAccountReference", "3001234567"); body.put("hours", List.of(schedule));
        mvc.perform(put("/api/v1/admin/settings").with(user("admin-ops@servifood.local").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tradeName").value("Distrito actualizado")).andExpect(jsonPath("$.hours[0].dayOfWeek").value("TUESDAY"));
    }

    private void transition(OrderStatus status, String reason) throws Exception {
        Map<String,Object> body = new HashMap<>(); body.put("status", status.name()); if (reason != null) body.put("reason", reason);
        mvc.perform(patch("/api/v1/admin/orders/{number}/status", order.getPublicNumber()).with(user("cashier-ops@servifood.local").roles("CASHIER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status.name()));
    }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
