package com.servifood.presentation.rest;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.*;
import com.servifood.application.AdminOrderService;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoyaltyApiTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired AdminOrderService orderService; @Autowired PasswordEncoder passwords;
    @Autowired CustomerRepository customers; @Autowired CustomerOrderRepository orders; @Autowired LoyaltyPointMovementRepository movements;
    @Autowired CouponRepository coupons; @Autowired CouponRedemptionRepository redemptions; @Autowired CategoryRepository categories;
    @Autowired ProductRepository products; @Autowired BusinessSettingsRepository businessSettings; @Autowired InternalUserRepository users;
    Product product; Customer customer; String adminEmail;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8); adminEmail = "admin-loyalty-" + suffix + "@servifood.local";
        users.save(new InternalUser("Admin Fidelización", adminEmail, passwords.encode("test-password"), UserRole.ADMIN));
        Category category = categories.save(new Category("Loyalty " + suffix, "loyalty-" + suffix, 1));
        product = products.save(new Product("Smash fidelidad " + suffix, "smash-fidelidad-" + suffix, "Producto de prueba", money("50000"), category));
        customer = customers.save(new Customer("Ana Cliente", "3005551212", "ana@example.com"));
        coupons.save(new Coupon("CLIENTE10", DiscountType.PERCENTAGE, money("10"), Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS), money("20000"), 10, 1, true));
        if (businessSettings.findFirstByOrderByIdAsc().isEmpty()) businessSettings.save(new BusinessSettings("Distrito Smash", "Demo", "3005550000", "3005550000", "Calle 1", BigDecimal.ZERO, 20, "COP"));
    }

    @Test
    void earnsOnceOnDeliveryThenRedeemsPointsAndCouponUsingBackendTotals() throws Exception {
        JsonNode first = createOrder(null, 0); String firstNumber = first.get("publicNumber").asText();
        transition(firstNumber, OrderStatus.CONFIRMED); transition(firstNumber, OrderStatus.PREPARING); transition(firstNumber, OrderStatus.READY); transition(firstNumber, OrderStatus.DELIVERED);
        assertEquals(50, customers.findById(customer.getId()).orElseThrow().getPointsBalance());
        assertEquals(1, movements.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream().filter(value -> value.getType() == LoyaltyMovementType.EARN).count());
        mvc.perform(patch("/api/v1/admin/orders/{number}/status", firstNumber).with(user(adminEmail).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}")) .andExpect(status().isBadRequest());
        assertEquals(50, customers.findById(customer.getId()).orElseThrow().getPointsBalance());

        Map<String,Object> quote = new LinkedHashMap<>(); quote.put("deliveryType", "PICKUP"); quote.put("lines", lines()); quote.put("customerPhone", customer.getPhone()); quote.put("couponCode", "cliente10"); quote.put("pointsToRedeem", 10);
        mvc.perform(post("/api/v1/public/orders/quote").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(quote)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totals.total").value(35000)).andExpect(jsonPath("$.loyalty.availablePoints").value(50))
                .andExpect(jsonPath("$.loyalty.couponDiscount").value(5000)).andExpect(jsonPath("$.loyalty.pointsDiscount").value(10000));

        JsonNode second = createOrder("CLIENTE10", 10); String secondNumber = second.get("publicNumber").asText();
        assertEquals(35000, second.get("totals").get("total").asInt()); assertEquals(40, customers.findById(customer.getId()).orElseThrow().getPointsBalance());
        assertEquals(1, redemptions.countByCouponIdAndReversedAtIsNull(coupons.findByCodeIgnoreCase("CLIENTE10").orElseThrow().getId()));
        mvc.perform(post("/api/v1/public/orders/quote").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(quote)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COUPON_CUSTOMER_LIMIT"));
        mvc.perform(get("/api/v1/admin/customers/{id}", customer.getId()).with(user(adminEmail).roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(2)).andExpect(jsonPath("$.orders[0].couponCode").value("CLIENTE10"))
                .andExpect(jsonPath("$.frequentProducts[0].name", containsString("Smash fidelidad")));

        transition(secondNumber, OrderStatus.CANCELLED);
        assertEquals(50, customers.findById(customer.getId()).orElseThrow().getPointsBalance());
        assertEquals(0, redemptions.countByCouponIdAndReversedAtIsNull(coupons.findByCodeIgnoreCase("CLIENTE10").orElseThrow().getId()));
        assertTrue(movements.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream().anyMatch(value -> value.getType() == LoyaltyMovementType.REVERSAL_REDEEM));
    }

    @Test
    void protectsMutationsAndAuditsManualPointAdjustments() throws Exception {
        String body = "{\"points\":15,\"reason\":\"Compensación por servicio\"}";
        mvc.perform(post("/api/v1/admin/customers/{id}/points", customer.getId()).with(user("cashier@test.local").roles("CASHIER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/customers/{id}/points", customer.getId()).with(user(adminEmail).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.points").value(15));
        mvc.perform(get("/api/v1/admin/customers").with(user("cashier@test.local").roles("CASHIER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/customers").with(user("kitchen@test.local").roles("KITCHEN"))).andExpect(status().isForbidden());
        LoyaltyPointMovement adjustment = movements.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).get(0); assertEquals("Compensación por servicio", adjustment.getReason()); assertNotNull(adjustment.getCreatedBy());
    }

    @Test
    void repeatOrderUsesCurrentPricesAndRejectsUnavailableProducts() throws Exception {
        String number = createOrder(null, 0).get("publicNumber").asText(); product.changePrice(money("55000")); products.saveAndFlush(product);
        mvc.perform(get("/api/v1/admin/customers/{id}/orders/{number}/repeat", customer.getId(), number).with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lines[0].price").value(55000));
        product.markUnavailable(); products.saveAndFlush(product);
        mvc.perform(get("/api/v1/admin/customers/{id}/orders/{number}/repeat", customer.getId(), number).with(user(adminEmail).roles("ADMIN")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));
    }

    private JsonNode createOrder(String coupon, int points) throws Exception {
        Map<String,Object> request = new LinkedHashMap<>(); request.put("clientRequestId", UUID.randomUUID()); request.put("customer", Map.of("name", customer.getName(), "phone", customer.getPhone(), "email", customer.getEmail()));
        request.put("delivery", Map.of("type", "PICKUP")); request.put("payment", Map.of("method", "PAY_ON_PICKUP")); request.put("lines", lines()); request.put("couponCode", coupon); request.put("pointsToRedeem", points);
        MockMultipartFile order = new MockMultipartFile("order", "order.json", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(request));
        return json.readTree(mvc.perform(multipart("/api/v1/public/orders").file(order)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private List<Map<String,Object>> lines() { return List.of(Map.of("productId", product.getId(), "quantity", 1, "notes", "", "extras", List.of())); }
    private void transition(String number, OrderStatus status) { orderService.changeStatus(number, status, status == OrderStatus.CANCELLED ? "Cancelación de prueba" : null); }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
