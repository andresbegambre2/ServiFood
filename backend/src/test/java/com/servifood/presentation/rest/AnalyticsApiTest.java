package com.servifood.presentation.rest;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsApiTest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder passwords;
    @Autowired CategoryRepository categories; @Autowired ProductRepository products; @Autowired CustomerRepository customers;
    @Autowired CustomerOrderRepository orders; @Autowired PaymentRepository payments; @Autowired BusinessSettingsRepository settings;
    @Autowired CouponRepository coupons; @Autowired CouponRedemptionRepository redemptions; @Autowired LoyaltyPointMovementRepository points;
    @Autowired IngredientRepository ingredients; @Autowired InternalUserRepository users;
    ZoneId zone = ZoneId.of("America/Bogota"); LocalDate today; Product product; Customer customer; Coupon coupon; String adminEmail;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(zone); String suffix = UUID.randomUUID().toString().substring(0, 8); adminEmail = "analytics-" + suffix + "@servifood.local";
        users.save(new InternalUser("Analista", adminEmail, passwords.encode("test-password"), UserRole.ADMIN));
        if (settings.findFirstByOrderByIdAsc().isEmpty()) settings.save(new BusinessSettings("Distrito", "Demo", "3005550000", "3005550000", "Calle 1", BigDecimal.ZERO, 20, "COP"));
        Category category = categories.save(new Category("Categoría analítica " + suffix, "analytics-" + suffix, 1)); product = products.save(new Product("Producto analítico " + suffix, "producto-analytics-" + suffix, "Demo", money("30000"), category));
        customer = customers.save(new Customer("Cliente frecuente " + suffix, "301" + suffix.replaceAll("[^0-9]", "") + "1234567", null));
        coupon = coupons.save(new Coupon("DATOS" + suffix.toUpperCase(), DiscountType.FIXED_AMOUNT, money("3000"), Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86400), BigDecimal.ZERO, null, null, true));
        CustomerOrder deliveredToday = order("TODAY-" + suffix, money("30000"), OrderStatus.DELIVERED, PaymentMethod.CASH, today, 14);
        order("YESTERDAY-" + suffix, money("50000"), OrderStatus.DELIVERED, PaymentMethod.TRANSFER, today.minusDays(1), 19);
        order("CANCEL-" + suffix, money("20000"), OrderStatus.CANCELLED, PaymentMethod.CASH, today, 14);
        CouponRedemption redemption = redemptions.saveAndFlush(new CouponRedemption(coupon, customer, deliveredToday, money("3000"))); at("coupon_redemptions", redemption.getId(), today, 14);
        customer.addPoints(30); customers.save(customer); LoyaltyPointMovement earned = points.saveAndFlush(new LoyaltyPointMovement(customer, deliveredToday, null, LoyaltyMovementType.EARN, 30, "Prueba")); at("loyalty_point_movements", earned.getId(), today, 14);
        customer.redeemPoints(5); customers.save(customer); LoyaltyPointMovement redeemed = points.saveAndFlush(new LoyaltyPointMovement(customer, null, null, LoyaltyMovementType.REDEEM, -5, "Prueba")); at("loyalty_point_movements", redeemed.getId(), today, 15);
        ingredients.save(new Ingredient("Insumo bajo " + suffix, IngredientUnit.UNIT, amount("2"), amount("5"), null)); ingredients.save(new Ingredient("Insumo agotado " + suffix, IngredientUnit.UNIT, amount("0"), amount("2"), null));
    }

    @Test
    void calculatesFilteredMetricsWithDatabaseAggregations() throws Exception {
        mvc.perform(get("/api/v1/admin/analytics").param("from", today.toString()).param("to", today.toString()).with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.today.sales").value(30000)).andExpect(jsonPath("$.today.previousSales").value(50000))
                .andExpect(jsonPath("$.today.changePercentage").value(-40)).andExpect(jsonPath("$.totalOrders").value(2)).andExpect(jsonPath("$.cancelledOrders").value(1))
                .andExpect(jsonPath("$.averageTicket").value(30000)).andExpect(jsonPath("$.couponUses").value(1)).andExpect(jsonPath("$.pointsEarned").value(30))
                .andExpect(jsonPath("$.pointsRedeemed").value(5)).andExpect(jsonPath("$.topProducts[0].label", containsString("Producto analítico")))
                .andExpect(jsonPath("$.topCategories[0].label", containsString("Categoría analítica"))).andExpect(jsonPath("$.paymentMethods[0].label").value("Efectivo"))
                .andExpect(jsonPath("$.deliveryTypes[0].label").value("Recoger")).andExpect(jsonPath("$.lowStockIngredients").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.outOfStockIngredients").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void filtersReportsAndExportsUtf8Csv() throws Exception {
        mvc.perform(get("/api/v1/admin/reports/PRODUCTS").param("from", today.toString()).param("to", today.toString()).with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.columns[0]").value("Producto")).andExpect(jsonPath("$.rows[0][0]", containsString("Producto analítico"))).andExpect(jsonPath("$.rows[0][1]").value(1));
        mvc.perform(get("/api/v1/admin/reports/ORDERS/csv").param("from", today.toString()).param("to", today.toString()).with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", containsString("servifood-orders.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv;charset=UTF-8")).andExpect(content().string(containsString("Pedido;Fecha;Estado;Cliente;Total;Descuento")))
                .andExpect(content().string(containsString("TODAY-"))).andExpect(content().string(not(containsString("YESTERDAY-"))));
    }

    @Test
    void grantsAdvancedAnalyticsOnlyToAdminAndValidatesRange() throws Exception {
        mvc.perform(get("/api/v1/admin/analytics").with(user("cashier@test.local").roles("CASHIER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/reports/SALES").with(user("kitchen@test.local").roles("KITCHEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/analytics").param("from", today.toString()).param("to", today.minusDays(1).toString()).with(user(adminEmail).roles("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/dashboard").with(user("cashier@test.local").roles("CASHIER"))).andExpect(status().isOk());
    }

    private CustomerOrder order(String number, BigDecimal price, OrderStatus target, PaymentMethod method, LocalDate date, int hour) {
        product.changePrice(price); Product current = products.save(product); CustomerOrder order = new CustomerOrder("SF-" + number, customer, customer.getName(), customer.getPhone(), DeliveryType.PICKUP, null, BigDecimal.ZERO); order.addItem(new OrderItem(current, 1, null));
        if (target == OrderStatus.DELIVERED) { order.confirm(); order.startPreparation(); order.markReady(); order.deliver(); } else order.cancel("Prueba analítica");
        order = orders.saveAndFlush(order); payments.saveAndFlush(new Payment(order, method, order.getTotal())); at("orders", order.getId(), date, hour); return order;
    }
    private void at(String table, Long id, LocalDate date, int hour) { jdbc.update("UPDATE " + table + " SET created_at = ?, updated_at = ? WHERE id = ?", Timestamp.from(date.atTime(hour, 0).atZone(zone).toInstant()), Timestamp.from(date.atTime(hour, 0).atZone(zone).toInstant()), id); }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); } private BigDecimal amount(String value) { return new BigDecimal(value).setScale(3); }
}
