package com.servifood.presentation.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicOrderApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CategoryRepository categories;
    @Autowired ExtraRepository extras;
    @Autowired ProductRepository products;
    @Autowired PromotionRepository promotions;
    @Autowired BusinessSettingsRepository settings;
    @Autowired BusinessHoursRepository hours;
    @Autowired CustomerOrderRepository orders;
    @Autowired PaymentRepository payments;
    Product product; Extra extra;

    @BeforeEach
    void setup() {
        Category category = categories.save(new Category("Checkout " + UUID.randomUUID(), "checkout-" + UUID.randomUUID(), 1));
        extra = extras.save(new Extra("Extra " + UUID.randomUUID(), money("3000")));
        product = new Product("Burger checkout", "burger-" + UUID.randomUUID(), "Producto de prueba", money("20000"), category);
        product.allowExtra(extra); product = products.save(product);
        BusinessSettings business = new BusinessSettings("Distrito Test", "Demo", "+573005551212", "+573005551212", "Calle 1", money("5000"), 25, "COP");
        business.configureCheckout("America/Bogota", "Nequi", "Distrito Test", "3005551212", "/qr.svg"); settings.save(business);
        hours.save(new BusinessHours(DayOfWeek.MONDAY, 1, LocalTime.of(0, 0), LocalTime.of(23, 59), false));
        promotions.save(new Promotion("Promo checkout", DiscountType.PERCENTAGE, money("10"), Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS), money("10000")));
    }

    @Test
    void createsDeliveryWithBackendTotalsSnapshotsExtrasAndCashPayment() throws Exception {
        JsonNode response = create(cashRequest(UUID.randomUUID(), "DELIVERY", "20000", "3000"), null, 201);
        assertThat(response.path("publicNumber").asText()).startsWith("SF-");
        assertThat(response.path("trackingToken").asText()).hasSizeGreaterThan(30);
        assertThat(response.at("/totals/subtotal").decimalValue()).isEqualByComparingTo("46000.00");
        assertThat(response.at("/totals/discount").decimalValue()).isEqualByComparingTo("4600.00");
        assertThat(response.at("/totals/deliveryFee").decimalValue()).isEqualByComparingTo("5000.00");
        assertThat(response.at("/totals/total").decimalValue()).isEqualByComparingTo("46400.00");
        CustomerOrder stored = orders.findByPublicNumber(response.path("publicNumber").asText()).orElseThrow();
        assertThat(stored.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductNameSnapshot()).isEqualTo("Burger checkout"); assertThat(item.getExtras()).singleElement().satisfies(value -> assertThat(value.getExtraNameSnapshot()).startsWith("Extra "));
        });
        Payment payment = payments.findFirstByOrderId(stored.getId()).orElseThrow();
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH); assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCashTendered()).isEqualByComparingTo("50000");
    }

    @Test
    void quotesAuthoritativeTotalsWithoutPersistingAnOrder() throws Exception {
        Map<String, Object> line = Map.of("productId", product.getId(), "quantity", 2, "notes", "Sin cebolla",
                "expectedUnitPrice", 1, "extras", List.of(Map.of("extraId", extra.getId(), "expectedUnitPrice", 1)));
        String content = mvc.perform(post("/api/v1/public/orders/quote").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("deliveryType", "DELIVERY", "lines", List.of(line)))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode response = json.readTree(content);
        assertThat(response.at("/totals/subtotal").decimalValue()).isEqualByComparingTo("46000.00");
        assertThat(response.at("/totals/total").decimalValue()).isEqualByComparingTo("46400.00");
        assertThat(orders.count()).isZero();
    }

    @Test
    void createsPickupWithoutDeliveryFee() throws Exception {
        JsonNode response = create(cashRequest(UUID.randomUUID(), "PICKUP", "20000", "3000"), null, 201);
        assertThat(response.at("/totals/deliveryFee").decimalValue()).isZero();
        assertThat(response.path("deliveryAddress").isNull()).isTrue();
        assertThat(response.path("paymentMethod").asText()).isEqualTo("PAY_ON_PICKUP");
    }

    @Test
    void storesValidTransferReceiptUnderAnInternalName() throws Exception {
        byte[] png = new byte[] {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        MockMultipartFile receipt = new MockMultipartFile("receipt", "../../proof.png", "image/png", png);
        JsonNode response = create(transferRequest(UUID.randomUUID()), receipt, 201);
        assertThat(response.path("paymentStatus").asText()).isEqualTo("UNDER_REVIEW");
        CustomerOrder order = orders.findByPublicNumber(response.path("publicNumber").asText()).orElseThrow();
        String path = payments.findFirstByOrderId(order.getId()).orElseThrow().getReceiptPath();
        assertThat(path).matches("[0-9a-f-]{36}\\.png").doesNotContain("proof", "..", "/", "\\");
    }

    @Test
    void rejectsInvalidReceiptContentAndDoesNotPersistOrder() throws Exception {
        MockMultipartFile receipt = new MockMultipartFile("receipt", "proof.png", "image/png", "not-an-image".getBytes(StandardCharsets.UTF_8));
        JsonNode response = create(transferRequest(UUID.randomUUID()), receipt, 415);
        assertThat(response.path("code").asText()).isEqualTo("INVALID_RECEIPT"); assertThat(orders.count()).isZero();
    }

    @Test
    void rejectsDeliveryWithoutAddressAndKeepsPersistenceClean() throws Exception {
        Map<String, Object> request = cashRequest(UUID.randomUUID(), "DELIVERY", "20000", "3000");
        request.put("delivery", Map.of("type", "DELIVERY", "address", "", "neighborhood", ""));
        JsonNode response = create(request, null, 400);
        assertThat(response.path("code").asText()).isEqualTo("INVALID_ADDRESS");
        assertThat(orders.count()).isZero();
    }

    @Test
    void rejectsExtrasThatAreNotAllowedForTheProduct() throws Exception {
        Extra forbidden = extras.save(new Extra("Forbidden " + UUID.randomUUID(), money("1000")));
        Map<String, Object> request = cashRequest(UUID.randomUUID(), "DELIVERY", "20000", "3000");
        @SuppressWarnings("unchecked") List<Map<String, Object>> lines = (List<Map<String, Object>>) request.get("lines");
        lines.getFirst().put("extras", List.of(Map.of("extraId", forbidden.getId(), "expectedUnitPrice", 1000)));
        JsonNode response = create(request, null, 409);
        assertThat(response.path("code").asText()).isEqualTo("EXTRA_UNAVAILABLE"); assertThat(orders.count()).isZero();
    }

    @Test
    void returnsSameOrderForDuplicateClientRequest() throws Exception {
        UUID requestId = UUID.randomUUID(); Map<String, Object> request = cashRequest(requestId, "DELIVERY", "20000", "3000");
        JsonNode first = create(request, null, 201); JsonNode second = create(request, null, 200);
        assertThat(second.path("publicNumber").asText()).isEqualTo(first.path("publicNumber").asText());
        assertThat(second.path("trackingToken").asText()).isEqualTo(first.path("trackingToken").asText());
        assertThat(second.path("idempotent").asBoolean()).isTrue(); assertThat(orders.count()).isOne();
    }

    @Test
    void rejectsUnavailableProductsAndChangedPrices() throws Exception {
        product.markUnavailable(); products.save(product);
        assertThat(create(cashRequest(UUID.randomUUID(), "DELIVERY", "20000", "3000"), null, 409).path("code").asText()).isEqualTo("PRODUCT_UNAVAILABLE");
    }

    @Test
    void returnsUpdatedQuoteWhenExpectedPriceChanged() throws Exception {
        JsonNode response = create(cashRequest(UUID.randomUUID(), "DELIVERY", "19000", "3000"), null, 409);
        assertThat(response.path("code").asText()).isEqualTo("PRICE_CHANGED");
        assertThat(response.at("/currentQuote/totals/total").decimalValue()).isEqualByComparingTo("46400.00");
        assertThat(orders.count()).isZero();
    }

    @Test
    void tracksOnlyWithPrivateToken() throws Exception {
        JsonNode created = create(cashRequest(UUID.randomUUID(), "DELIVERY", "20000", "3000"), null, 201);
        String number = created.path("publicNumber").asText(); String token = created.path("trackingToken").asText();
        mvc.perform(get("/api/v1/public/orders/{number}", number).param("token", "wrong")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/public/orders/{number}", number).param("token", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.publicNumber").value(number)).andExpect(jsonPath("$.customerName").value("Ana Cliente"))
                .andExpect(jsonPath("$.customerPhone").doesNotExist()).andExpect(jsonPath("$.trackingToken").doesNotExist());
    }

    @Test
    void keepsAdministrativeRoutesProtected() throws Exception {
        mvc.perform(get("/api/v1/admin/orders")).andExpect(status().isForbidden());
    }

    private JsonNode create(Map<String, Object> request, MockMultipartFile receipt, int status) throws Exception {
        MockMultipartFile orderPart = new MockMultipartFile("order", "order.json", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(request));
        var builder = multipart("/api/v1/public/orders").file(orderPart); if (receipt != null) builder.file(receipt);
        String content = mvc.perform(builder).andExpect(status().is(status)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(content);
    }
    private Map<String, Object> cashRequest(UUID id, String deliveryType, String productPrice, String extraPrice) {
        String method = deliveryType.equals("DELIVERY") ? "CASH" : "PAY_ON_PICKUP";
        return request(id, deliveryType, method, productPrice, extraPrice);
    }
    private Map<String, Object> transferRequest(UUID id) { return request(id, "DELIVERY", "TRANSFER", "20000", "3000"); }
    private Map<String, Object> request(UUID id, String deliveryType, String method, String productPrice, String extraPrice) {
        return new java.util.HashMap<>(Map.of("clientRequestId", id.toString(), "customer", Map.of("name", "Ana Cliente", "phone", "300 555 1212", "email", "ana@example.com"),
                "delivery", Map.of("type", deliveryType, "address", "Calle 10 # 20-30", "neighborhood", "Centro", "reference", "Apto 2"),
                "payment", Map.of("method", method, "cashTendered", method.equals("CASH") ? 50000 : 0),
                "lines", new java.util.ArrayList<>(List.of(new java.util.HashMap<>(Map.of("productId", product.getId(), "quantity", 2, "notes", "Sin cebolla", "expectedUnitPrice", new BigDecimal(productPrice),
                        "extras", List.of(Map.of("extraId", extra.getId(), "expectedUnitPrice", new BigDecimal(extraPrice)))))))));
    }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
