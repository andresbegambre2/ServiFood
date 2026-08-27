package com.servifood.presentation.rest;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import com.servifood.application.AdminOrderService;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryApiTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired PasswordEncoder passwords; @Autowired AdminOrderService orderService;
    @Autowired InternalUserRepository users; @Autowired CategoryRepository categories; @Autowired ProductRepository products; @Autowired ExtraRepository extras;
    @Autowired IngredientRepository ingredients; @Autowired ProductRecipeRepository productRecipes; @Autowired ExtraRecipeRepository extraRecipes;
    @Autowired CustomerOrderRepository orders; @Autowired InventoryMovementRepository movements;
    Product product; Extra extra; Ingredient ingredient; Category category; String suffix; boolean persistent;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        users.save(new InternalUser("Admin Inventory " + suffix, "admin-inventory-" + suffix + "@servifood.local", passwords.encode("test-password"), UserRole.ADMIN));
        category = categories.save(new Category("Inventory " + suffix, "inventory-" + suffix, 1));
        extra = extras.save(new Extra("Extra inventory " + suffix, money("1000")));
        product = new Product("Producto inventory " + suffix, "producto-inventory-" + suffix, "Prueba de receta", money("10000"), category); product.allowExtra(extra); product = products.save(product);
        ingredient = ingredients.save(new Ingredient("Ingrediente " + suffix, IngredientUnit.GRAM, amount("10"), amount("4"), new BigDecimal("2.5000")));
        productRecipes.save(new ProductRecipeIngredient(product, ingredient, amount("2")));
        extraRecipes.save(new ExtraRecipeIngredient(extra, ingredient, amount("1")));
    }

    @AfterEach
    void cleanPersistentConcurrencyData() {
        if (!persistent) return;
        CustomerOrder savedOrder = orders.findAllByOrderByCreatedAtDesc().stream().filter(value -> value.getPublicNumber().startsWith("SF-INV-")).findFirst().orElse(null);
        if (savedOrder != null) { movements.deleteAll(movements.findByOrderIdAndTypeOrderByIngredientIdAsc(savedOrder.getId(), InventoryMovementType.CONSUMPTION)); movements.deleteAll(movements.findByOrderIdAndTypeOrderByIngredientIdAsc(savedOrder.getId(), InventoryMovementType.REVERSAL)); orders.delete(savedOrder); }
        productRecipes.deleteAll(productRecipes.findByProductIdOrderByIngredientNameAsc(product.getId())); extraRecipes.deleteAll(extraRecipes.findByExtraIdOrderByIngredientNameAsc(extra.getId()));
        products.delete(product); extras.delete(extra); ingredients.deleteById(ingredient.getId()); categories.delete(category); users.findByEmailIgnoreCase(adminEmail()).ifPresent(users::delete);
    }

    @Test
    void consumesOnceOnPreparationAndReversesOnCancellation() throws Exception {
        CustomerOrder order = order(2, true); transition(order, "CONFIRMED", null);
        transition(order, "PREPARING", null);
        Ingredient consumed = ingredients.findById(ingredient.getId()).orElseThrow();
        assertEquals(amount("4"), consumed.getStockCurrent());
        assertEquals(1, movements.findByOrderIdAndTypeOrderByIngredientIdAsc(order.getId(), InventoryMovementType.CONSUMPTION).size());
        mvc.perform(get("/api/v1/admin/inventory").with(user(adminEmail()).roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients[?(@.name == '" + ingredient.getName() + "')].stockStatus", contains("LOW")))
                .andExpect(jsonPath("$.recentMovements[?(@.orderNumber == '" + order.getPublicNumber() + "')].type", contains("CONSUMPTION")));
        mvc.perform(patch("/api/v1/admin/orders/{number}/status", order.getPublicNumber()).with(user(adminEmail()).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PREPARING\"}")) .andExpect(status().isBadRequest());
        assertEquals(1, movements.findByOrderIdAndTypeOrderByIngredientIdAsc(order.getId(), InventoryMovementType.CONSUMPTION).size());
        transition(order, "CANCELLED", "Prueba de reversión");
        assertEquals(amount("10"), ingredients.findById(ingredient.getId()).orElseThrow().getStockCurrent());
        assertEquals(1, movements.findByOrderIdAndTypeOrderByIngredientIdAsc(order.getId(), InventoryMovementType.REVERSAL).size());
    }

    @Test
    void restrictsMutationsToAdminAndSupportsManualAdjustmentsAndRecipes() throws Exception {
        String body = json.writeValueAsString(Map.of("type", "ENTRY", "quantity", 5, "reason", "Compra semanal"));
        mvc.perform(post("/api/v1/admin/inventory/ingredients/{id}/adjustments", ingredient.getId()).with(user("cashier@servifood.local").roles("CASHIER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/inventory/ingredients/{id}/adjustments", ingredient.getId()).with(user(adminEmail()).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.stockCurrent").value(15));
        mvc.perform(get("/api/v1/admin/inventory").with(user("cashier@servifood.local").roles("CASHIER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/inventory").with(user("kitchen@servifood.local").roles("KITCHEN"))).andExpect(status().isForbidden());
        Map<String,Object> recipe = Map.of("ingredients", List.of(Map.of("ingredientId", ingredient.getId(), "quantity", 3)));
        mvc.perform(put("/api/v1/admin/inventory/recipes/products/{id}", product.getId()).with(user(adminEmail()).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(recipe)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ingredients[0].quantity").value(3));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void serializesConcurrentPreparationAndNeverDoubleConsumes() throws Exception {
        persistent = true;
        CustomerOrder order = order(1, false); order.confirm(); orders.saveAndFlush(order);
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1); AtomicInteger success = new AtomicInteger();
        Callable<Void> task = () -> { ready.countDown(); start.await(5, TimeUnit.SECONDS); try { orderService.changeStatus(order.getPublicNumber(), OrderStatus.PREPARING, null); success.incrementAndGet(); } catch (RuntimeException expected) { /* one transition must lose */ } return null; };
        Future<Void> first = executor.submit(task); Future<Void> second = executor.submit(task); assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown(); first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS); executor.shutdownNow();
        assertEquals(1, success.get());
        assertEquals(amount("8"), ingredients.findById(ingredient.getId()).orElseThrow().getStockCurrent());
        assertEquals(1, movements.findByOrderIdAndTypeOrderByIngredientIdAsc(order.getId(), InventoryMovementType.CONSUMPTION).size());
    }

    @Test
    void hidesProductsWhenARecipeCannotBePrepared() throws Exception {
        ingredient.adjust(amount("-9")); ingredients.saveAndFlush(ingredient);
        mvc.perform(get("/api/v1/public/products")).andExpect(status().isOk()).andExpect(content().string(not(containsString(product.getSlug()))));
        mvc.perform(get("/api/v1/public/products/{slug}", product.getSlug())).andExpect(status().isNotFound());
    }

    private CustomerOrder order(int quantity, boolean withExtra) {
        OrderItem item = new OrderItem(product, quantity, null); if (withExtra) item.addExtra(new OrderItemExtra(extra, quantity));
        CustomerOrder order = new CustomerOrder("SF-INV-" + UUID.randomUUID().toString().substring(0, 8), null, "Cliente Inventario", "3001234567", DeliveryType.PICKUP, null, BigDecimal.ZERO); order.addItem(item); return orders.saveAndFlush(order);
    }
    private void transition(CustomerOrder order, String status, String reason) throws Exception { Map<String,Object> body = new HashMap<>(); body.put("status", status); if (reason != null) body.put("reason", reason); mvc.perform(patch("/api/v1/admin/orders/{number}/status", order.getPublicNumber()).with(user(adminEmail()).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isOk()); }
    private String adminEmail() { return "admin-inventory-" + suffix + "@servifood.local"; }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
    private BigDecimal amount(String value) { return new BigDecimal(value).setScale(3); }
}
