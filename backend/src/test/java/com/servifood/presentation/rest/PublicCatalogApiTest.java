package com.servifood.presentation.rest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicCatalogApiTest {
    @Autowired MockMvc mockMvc;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ExtraRepository extras;
    @Autowired PromotionRepository promotions;
    @Autowired BusinessSettingsRepository settings;
    @Autowired BusinessHoursRepository hours;

    @Test
    void returnsOnlyActiveCategoriesAndAvailableProducts() throws Exception {
        Category active = categories.save(new Category("Activa API", "activa-api", 1));
        Category hidden = new Category("Oculta API", "oculta-api", 2); hidden.deactivate(); categories.save(hidden);
        Product visible = new Product("Visible API", "visible-api", "Producto visible", money("25000"), active); visible.markFeatured(); products.save(visible);
        Product unavailable = new Product("Agotado API", "agotado-api", "Producto agotado", money("20000"), active); unavailable.markUnavailable(); products.save(unavailable);

        mockMvc.perform(get("/api/v1/public/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("activa-api")));
        mockMvc.perform(get("/api/v1/public/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("visible-api")))
                .andExpect(content().string(not(containsString("agotado-api"))));
        mockMvc.perform(get("/api/v1/public/products/featured")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].featured").value(true));
    }

    @Test
    void exposesProductDetailsAndAllowedExtrasWithoutInternalFields() throws Exception {
        Category category = categories.save(new Category("Detalle API", "detalle-api", 1));
        Extra extra = extras.save(new Extra("Cheddar API", money("3000")));
        Product product = new Product("Burger API", "burger-api", "Detalle público", money("24000"), category); product.allowExtra(extra); products.save(product);
        mockMvc.perform(get("/api/v1/public/products/burger-api")).andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedExtras[0].name").value("Cheddar API"))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("createdAt"))));
    }

    @Test
    void returnsOnlyCurrentlyActivePromotions() throws Exception {
        Instant now = Instant.now();
        Promotion current = new Promotion("Vigente API", DiscountType.PERCENTAGE, money("10"), now.minusSeconds(60), now.plusSeconds(3600), BigDecimal.ZERO);
        Promotion expired = new Promotion("Vencida API", DiscountType.FIXED_AMOUNT, money("5000"), now.minusSeconds(7200), now.minusSeconds(3600), BigDecimal.ZERO);
        promotions.save(current); promotions.save(expired);
        mockMvc.perform(get("/api/v1/public/promotions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Vigente API")))
                .andExpect(content().string(not(containsString("Vencida API"))));
    }

    @Test
    void exposesPublicBusinessConfigurationAndSchedule() throws Exception {
        BusinessSettings business = new BusinessSettings("Distrito Test", "Descripción", "3001234567", "3001234567", "Calle 1", money("5000"), 25, "COP"); settings.save(business);
        hours.save(new BusinessHours(DayOfWeek.TUESDAY, 1, LocalTime.of(12, 0), LocalTime.of(22, 0), false));
        mockMvc.perform(get("/api/v1/public/business")).andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeName").value("Distrito Test"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.hours[0].dayOfWeek").value("TUESDAY"))
                .andExpect(content().string(not(containsString("password"))));
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
