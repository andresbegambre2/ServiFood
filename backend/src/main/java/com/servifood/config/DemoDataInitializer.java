package com.servifood.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final PromotionRepository promotions;
    private final IngredientRepository ingredients;
    private final ProductRecipeRepository productRecipes;
    private final ExtraRecipeRepository extraRecipes;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataInitializer(CategoryRepository categories, ExtraRepository extras, ProductRepository products,
            InternalUserRepository users, BusinessSettingsRepository settings, BusinessHoursRepository hours, PromotionRepository promotions,
            IngredientRepository ingredients, ProductRecipeRepository productRecipes, ExtraRecipeRepository extraRecipes,
            PasswordEncoder passwordEncoder, @Value("${app.demo.admin-password}") String demoPassword) {
        this.categories = categories; this.extras = extras; this.products = products; this.users = users;
        this.settings = settings; this.hours = hours; this.promotions = promotions; this.ingredients = ingredients; this.productRecipes = productRecipes; this.extraRecipes = extraRecipes; this.passwordEncoder = passwordEncoder; this.demoPassword = demoPassword;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        ensureUsers();
        if (categories.count() > 0) {
            enrichExistingDemoSettings();
            ensureDemoInventory();
            return;
        }
        List<Category> categoryList = categories.saveAll(List.of(
                new Category("Hamburguesas", "hamburguesas", 1), new Category("Combos", "combos", 2),
                new Category("Acompañamientos", "acompanamientos", 3), new Category("Bebidas", "bebidas", 4),
                new Category("Postres", "postres", 5)));
        Extra meat = new Extra("Carne adicional", money("8000")); Extra cheddar = new Extra("Cheddar", money("3000"));
        Extra bacon = new Extra("Tocineta", money("4000")); Extra jalapeno = new Extra("Jalapeños", money("2000"));
        Extra sauce = new Extra("Salsa adicional", money("1500")); extras.saveAll(List.of(meat, cheddar, bacon, jalapeno, sauce));
        Category burgers = categoryList.get(0), combos = categoryList.get(1), sides = categoryList.get(2), drinks = categoryList.get(3), desserts = categoryList.get(4);
        Product classic = photo(product("Clásica Urbana", "clasica-urbana", "Carne smash, cheddar, pepinillos y salsa Distrito.", "22000", burgers, meat, cheddar, bacon, jalapeno, sauce), "/images/products/double-smash.webp", true);
        Product doubleBurger = photo(product("Doble Bacon", "doble-bacon", "Doble carne, cheddar fundido y tocineta crocante.", "32000", burgers, meat, cheddar, bacon, sauce), "/images/products/double-smash.webp", true);
        Product loadedFries = photo(product("Papas Distrito", "papas-distrito", "Papas rústicas, cheddar, tocineta y cebollín.", "16000", sides, cheddar, bacon, sauce), "/images/products/loaded-fries.webp", true);
        products.saveAll(List.of(classic, doubleBurger,
                photo(product("Pollo Crispy", "pollo-crispy", "Pollo crujiente, ensalada fresca y mayonesa especiada.", "24000", burgers, cheddar, jalapeno, sauce), "/images/products/combo-smash.webp", false),
                photo(product("Smoky BBQ", "smoky-bbq", "Carne smash, aros de cebolla, cheddar y BBQ ahumada.", "27000", burgers, meat, cheddar, bacon), "/images/products/double-smash.webp", true),
                photo(product("Fuego Norte", "fuego-norte", "Doble carne, jalapeños, cheddar y salsa picante de la casa.", "29000", burgers, meat, cheddar, jalapeno), "/images/products/double-smash.webp", false),
                photo(product("Verde Rebelde", "verde-rebelde", "Medallón vegetal, vegetales asados y salsa de hierbas.", "23000", burgers, cheddar, jalapeno, sauce), "/images/products/combo-smash.webp", false),
                photo(product("Combo Callejero", "combo-callejero", "Clásica Urbana, papas rústicas y bebida.", "30000", combos, cheddar, bacon, sauce), "/images/products/combo-smash.webp", true),
                photo(product("Combo Doble Turno", "combo-doble-turno", "Doble Bacon, papas rústicas y bebida.", "40000", combos, meat, cheddar, bacon, sauce), "/images/products/combo-smash.webp", false),
                photo(product("Combo Crispy", "combo-crispy", "Pollo Crispy, papas y limonada artesanal.", "33000", combos, cheddar, jalapeno, sauce), "/images/products/combo-smash.webp", false),
                loadedFries,
                photo(product("Aros de cebolla", "aros-de-cebolla", "Aros extra crocantes con salsa ahumada.", "11000", sides, sauce), "/images/products/loaded-fries.webp", false),
                product("Limonada artesanal", "limonada-artesanal", "Limón fresco, hierbabuena y el punto justo de dulce.", "7000", drinks),
                product("Soda de frutos rojos", "soda-frutos-rojos", "Soda fría con frutos rojos y cítricos.", "9000", drinks),
                product("Brownie tibio", "brownie-tibio", "Chocolate intenso y centro suave.", "10000", desserts),
                product("Cheesecake de maracuyá", "cheesecake-maracuya", "Cremoso, fresco y ligeramente ácido.", "12000", desserts)));
        BusinessSettings demoSettings = new BusinessSettings("Distrito Smash", "Fuego, barrio y hamburguesas hechas sin atajos.", "+57 300 555 0147", "+57 300 555 0147", "Carrera 13 # 74-21, Bogotá", money("5000"), 25, "COP");
        demoSettings.setSocialLinks("https://instagram.com/distritosmash", "https://facebook.com/distritosmash");
        demoSettings.configureCheckout("America/Bogota", "Nequi", "Distrito Smash Demo", "300 555 0147", "/images/payment-qr-demo.svg");
        settings.save(demoSettings);
        Promotion promotion = new Promotion("Doble noche", DiscountType.PERCENTAGE, money("15"), Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS), money("30000"));
        promotion.setDescription("15% de descuento en pedidos desde $30.000. Solo por tiempo limitado."); promotions.save(promotion);
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean closed = day == DayOfWeek.MONDAY;
            hours.save(new BusinessHours(day, 1, closed ? null : LocalTime.of(12, 0), closed ? null : LocalTime.of(22, 0), closed));
        }
        ensureDemoInventory();
    }

    private void ensureUsers() {
        createUser("Administrador Demo", "admin@servifood.local", UserRole.ADMIN);
        createUser("Caja Demo", "cashier@servifood.local", UserRole.CASHIER);
        createUser("Cocina Demo", "kitchen@servifood.local", UserRole.KITCHEN);
    }

    private void createUser(String name, String email, UserRole role) {
        if (users.findByEmailIgnoreCase(email).isEmpty()) users.save(new InternalUser(name, email, passwordEncoder.encode(demoPassword), role));
    }

    private void enrichExistingDemoSettings() {
        settings.findFirstByOrderByIdAsc().ifPresent(current -> {
            if (current.getTransferProvider() == null || current.getTransferProvider().isBlank()) {
                current.configureCheckout("America/Bogota", "Nequi", "Distrito Smash Demo", "300 555 0147", "/images/payment-qr-demo.svg");
            }
        });
    }

    private void ensureDemoInventory() {
        Ingredient meat = ingredient("Carne de res", IngredientUnit.GRAM, "8000", "1000", "32");
        Ingredient bread = ingredient("Pan brioche", IngredientUnit.UNIT, "80", "12", "1200");
        Ingredient cheese = ingredient("Queso cheddar", IngredientUnit.GRAM, "5000", "800", "28");
        Ingredient potatoes = ingredient("Papa rústica", IngredientUnit.GRAM, "10000", "1500", "6");
        Ingredient bacon = ingredient("Tocineta", IngredientUnit.GRAM, "3000", "500", "35");
        Ingredient sauce = ingredient("Salsa Distrito", IngredientUnit.MILLILITER, "4000", "600", "8");
        Ingredient jalapeno = ingredient("Jalapeño", IngredientUnit.GRAM, "300", "500", "18");
        productRecipe("clasica-urbana", new Object[][] {{meat, "150"}, {bread, "1"}, {cheese, "30"}, {sauce, "20"}});
        productRecipe("doble-bacon", new Object[][] {{meat, "300"}, {bread, "1"}, {cheese, "50"}, {bacon, "30"}});
        productRecipe("papas-distrito", new Object[][] {{potatoes, "250"}, {cheese, "40"}, {bacon, "30"}});
        productRecipe("combo-callejero", new Object[][] {{meat, "150"}, {bread, "1"}, {cheese, "30"}, {potatoes, "250"}, {sauce, "20"}});
        extraRecipe("Carne adicional", meat, "150"); extraRecipe("Cheddar", cheese, "30"); extraRecipe("Tocineta", bacon, "30");
        extraRecipe("Salsa adicional", sauce, "20"); extraRecipe("Jalapeños", jalapeno, "20");
    }

    private Ingredient ingredient(String name, IngredientUnit unit, String current, String minimum, String cost) {
        return ingredients.findByNameIgnoreCase(name).orElseGet(() -> ingredients.save(new Ingredient(name, unit, decimal(current), decimal(minimum), decimal(cost))));
    }
    private void productRecipe(String slug, Object[][] lines) { products.findBySlug(slug).ifPresent(product -> { if (productRecipes.findByProductIdOrderByIngredientNameAsc(product.getId()).isEmpty()) for (Object[] line : lines) productRecipes.save(new ProductRecipeIngredient(product, (Ingredient) line[0], decimal((String) line[1]))); }); }
    private void extraRecipe(String name, Ingredient ingredient, String quantity) { extras.findAll().stream().filter(extra -> extra.getName().equals(name)).findFirst().ifPresent(extra -> { if (extraRecipes.findByExtraIdOrderByIngredientNameAsc(extra.getId()).isEmpty()) extraRecipes.save(new ExtraRecipeIngredient(extra, ingredient, decimal(quantity))); }); }
    private BigDecimal decimal(String value) { return new BigDecimal(value).setScale(3); }

    private Product product(String name, String slug, String description, String price, Category category, Extra... allowed) {
        Product product = new Product(name, slug, description, money(price), category); for (Extra extra : allowed) product.allowExtra(extra); return product;
    }
    private Product photo(Product product, String imagePath, boolean featured) { product.setImagePath(imagePath); if (featured) product.markFeatured(); return product; }
    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
