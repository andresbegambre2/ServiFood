package com.servifood.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Component
@Profile("dev")
public class DemoDataInitializer implements ApplicationRunner {
    private final CategoryRepository categories;
    private final ExtraRepository extras;
    private final ProductRepository products;
    private final InternalUserRepository users;
    private final BusinessSettingsRepository settings;
    private final BusinessHoursRepository hours;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataInitializer(CategoryRepository categories, ExtraRepository extras, ProductRepository products,
            InternalUserRepository users, BusinessSettingsRepository settings, BusinessHoursRepository hours,
            PasswordEncoder passwordEncoder, @Value("${app.demo.admin-password:demo-only-change-me}") String demoPassword) {
        this.categories = categories; this.extras = extras; this.products = products; this.users = users;
        this.settings = settings; this.hours = hours; this.passwordEncoder = passwordEncoder; this.demoPassword = demoPassword;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (categories.count() > 0) return;
        List<Category> categoryList = categories.saveAll(List.of(
                new Category("Hamburguesas", "hamburguesas", 1), new Category("Combos", "combos", 2),
                new Category("Acompañamientos", "acompanamientos", 3), new Category("Bebidas", "bebidas", 4),
                new Category("Postres", "postres", 5)));
        Extra meat = new Extra("Carne adicional", money("8000")); Extra cheddar = new Extra("Cheddar", money("3000"));
        Extra bacon = new Extra("Tocineta", money("4000")); Extra jalapeno = new Extra("Jalapeños", money("2000"));
        Extra sauce = new Extra("Salsa adicional", money("1500")); extras.saveAll(List.of(meat, cheddar, bacon, jalapeno, sauce));
        Category burgers = categoryList.get(0), combos = categoryList.get(1), sides = categoryList.get(2), drinks = categoryList.get(3), desserts = categoryList.get(4);
        Product classic = product("Clásica Urbana", "clasica-urbana", "Carne, queso, vegetales frescos y salsa de la casa.", "22000", burgers, meat, cheddar, bacon, jalapeno, sauce);
        Product doubleBurger = product("Doble Bacon", "doble-bacon", "Doble carne, cheddar y tocineta crocante.", "32000", burgers, meat, cheddar, bacon, sauce);
        products.saveAll(List.of(classic, doubleBurger,
                product("Pollo Crispy", "pollo-crispy", "Pollo crujiente, ensalada y mayonesa especiada.", "24000", burgers, cheddar, jalapeno, sauce),
                product("Combo Callejero", "combo-callejero", "Hamburguesa clásica, papas y bebida.", "30000", combos, cheddar, bacon, sauce),
                product("Papas rústicas", "papas-rusticas", "Papas doradas con sal de la casa.", "9000", sides, cheddar, bacon, sauce),
                product("Aros de cebolla", "aros-de-cebolla", "Aros crocantes con salsa ahumada.", "11000", sides, sauce),
                product("Limonada artesanal", "limonada-artesanal", "Limonada natural recién preparada.", "7000", drinks),
                product("Brownie tibio", "brownie-tibio", "Brownie de chocolate con centro suave.", "10000", desserts)));
        users.save(new InternalUser("Administrador Demo", "admin@servifood.local", passwordEncoder.encode(demoPassword), UserRole.ADMIN));
        settings.save(new BusinessSettings("ServiFood Urban Burger", "Hamburguesas con actitud y sabor local.", "+57 300 000 0000", "+57 300 000 0000", "Calle Demo 123, Bogotá", money("5000"), 25, "COP"));
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean closed = day == DayOfWeek.MONDAY;
            hours.save(new BusinessHours(day, 1, closed ? null : LocalTime.of(12, 0), closed ? null : LocalTime.of(22, 0), closed));
        }
    }

    private Product product(String name, String slug, String description, String price, Category category, Extra... allowed) {
        Product product = new Product(name, slug, description, money(price), category); for (Extra extra : allowed) product.allowExtra(extra); return product;
    }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
