package com.servifood.application;

import static com.servifood.presentation.rest.dto.InventoryDtos.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.*;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
public class AdminInventoryService {
    private final IngredientRepository ingredients; private final ProductRepository products; private final ExtraRepository extras;
    private final ProductRecipeRepository productRecipes; private final ExtraRecipeRepository extraRecipes;
    private final InventoryMovementRepository movements; private final InternalUserRepository users; private final InventoryAvailabilityService availability;
    public AdminInventoryService(IngredientRepository ingredients, ProductRepository products, ExtraRepository extras,
            ProductRecipeRepository productRecipes, ExtraRecipeRepository extraRecipes, InventoryMovementRepository movements,
            InternalUserRepository users, InventoryAvailabilityService availability) {
        this.ingredients = ingredients; this.products = products; this.extras = extras; this.productRecipes = productRecipes;
        this.extraRecipes = extraRecipes; this.movements = movements; this.users = users; this.availability = availability;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        List<IngredientView> ingredientViews = ingredients.findAllByOrderByNameAsc().stream().map(this::view).toList();
        List<Product> productValues = products.findAllByOrderByNameAsc(); List<Extra> extraValues = extras.findAllByOrderByNameAsc();
        Map<Long, List<ProductRecipeIngredient>> productLines = productRecipes.findByProductIdInOrderByIngredientNameAsc(productValues.stream().map(Product::getId).toList()).stream().collect(java.util.stream.Collectors.groupingBy(line -> line.getProduct().getId()));
        Map<Long, List<ExtraRecipeIngredient>> extraLines = extraRecipes.findByExtraIdInOrderByIngredientNameAsc(extraValues.stream().map(Extra::getId).toList()).stream().collect(java.util.stream.Collectors.groupingBy(line -> line.getExtra().getId()));
        Map<Long, Boolean> productAvailability = availability.canPrepareProducts(productValues); Map<Long, Boolean> extraAvailability = availability.canPrepareExtras(extraValues);
        return new Overview(ingredientViews.size(), ingredients.countLowStock(), ingredients.countOutOfStock(), ingredientViews,
                productValues.stream().map(value -> recipe(value, productLines.getOrDefault(value.getId(), List.of()), productAvailability.getOrDefault(value.getId(), false))).toList(),
                extraValues.stream().map(value -> recipe(value, extraLines.getOrDefault(value.getId(), List.of()), extraAvailability.getOrDefault(value.getId(), false))).toList(),
                movements.findTop200ByOrderByCreatedAtDesc().stream().map(this::movement).toList());
    }

    @Transactional
    public IngredientView create(CreateIngredient request, Authentication authentication) {
        Ingredient ingredient = ingredients.save(new Ingredient(request.name(), request.unit(), request.initialStock(), request.stockMinimum(), request.unitCost()));
        ingredient.update(request.name(), request.unit(), request.stockMinimum(), request.unitCost(), request.active());
        if (request.initialStock().signum() > 0) movements.save(new InventoryMovement(ingredient, null, user(authentication), InventoryMovementType.ENTRY, request.initialStock(), "Stock inicial"));
        return view(ingredient);
    }

    @Transactional
    public IngredientView update(Long id, UpdateIngredient request) { Ingredient ingredient = ingredient(id); ingredient.update(request.name(), request.unit(), request.stockMinimum(), request.unitCost(), request.active()); return view(ingredient); }

    @Transactional
    public IngredientView adjust(Long id, StockAdjustment request, Authentication authentication) {
        Ingredient ingredient = ingredients.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Ingredient", id.toString()));
        ingredient.adjust(request.quantity()); movements.save(new InventoryMovement(ingredient, null, user(authentication), request.type(), request.quantity(), request.reason())); return view(ingredient);
    }

    @Transactional
    public RecipeView replaceProductRecipe(Long id, RecipeRequest request) {
        Product product = products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        Map<Long, Ingredient> selected = selectedIngredients(request); productRecipes.deleteByProductId(id); productRecipes.flush();
        productRecipes.saveAll(request.ingredients().stream().map(line -> new ProductRecipeIngredient(product, selected.get(line.ingredientId()), line.quantity())).toList());
        return recipe(product);
    }

    @Transactional
    public RecipeView replaceExtraRecipe(Long id, RecipeRequest request) {
        Extra extra = extras.findById(id).orElseThrow(() -> new ResourceNotFoundException("Extra", id.toString()));
        Map<Long, Ingredient> selected = selectedIngredients(request); extraRecipes.deleteByExtraId(id); extraRecipes.flush();
        extraRecipes.saveAll(request.ingredients().stream().map(line -> new ExtraRecipeIngredient(extra, selected.get(line.ingredientId()), line.quantity())).toList());
        return recipe(extra);
    }

    private Map<Long, Ingredient> selectedIngredients(RecipeRequest request) {
        Map<Long, Ingredient> values = new LinkedHashMap<>();
        for (RecipeLineRequest line : request.ingredients()) { if (values.containsKey(line.ingredientId())) throw new DomainException("Un ingrediente no puede repetirse en la receta"); values.put(line.ingredientId(), ingredient(line.ingredientId())); }
        return values;
    }
    private Ingredient ingredient(Long id) { return ingredients.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ingredient", id.toString())); }
    private InternalUser user(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(); }
    private IngredientView view(Ingredient value) { return new IngredientView(value.getId(), value.getName(), value.getUnit(), value.getStockCurrent(), value.getStockMinimum(), value.getUnitCost(), value.isActive(), !value.isActive() ? "INACTIVE" : value.isOutOfStock() ? "OUT" : value.isLowStock() ? "LOW" : "OK"); }
    private RecipeView recipe(Product value) { return new RecipeView(value.getId(), value.getName(), availability.canPrepare(value), productRecipes.findByProductIdOrderByIngredientNameAsc(value.getId()).stream().map(this::line).toList()); }
    private RecipeView recipe(Extra value) { return new RecipeView(value.getId(), value.getName(), availability.canPrepare(value), extraRecipes.findByExtraIdOrderByIngredientNameAsc(value.getId()).stream().map(this::line).toList()); }
    private RecipeView recipe(Product value, List<ProductRecipeIngredient> lines, boolean available) { return new RecipeView(value.getId(), value.getName(), available, lines.stream().map(this::line).toList()); }
    private RecipeView recipe(Extra value, List<ExtraRecipeIngredient> lines, boolean available) { return new RecipeView(value.getId(), value.getName(), available, lines.stream().map(this::line).toList()); }
    private RecipeLine line(ProductRecipeIngredient value) { return new RecipeLine(value.getIngredient().getId(), value.getIngredient().getName(), value.getIngredient().getUnit(), value.getQuantity()); }
    private RecipeLine line(ExtraRecipeIngredient value) { return new RecipeLine(value.getIngredient().getId(), value.getIngredient().getName(), value.getIngredient().getUnit(), value.getQuantity()); }
    private MovementView movement(InventoryMovement value) { return new MovementView(value.getId(), value.getIngredient().getId(), value.getIngredient().getName(), value.getType(), value.getQuantityDelta(), value.getBalanceAfter(), value.getReason(), value.getOrder() == null ? null : value.getOrder().getPublicNumber(), value.getCreatedBy() == null ? null : value.getCreatedBy().getName(), value.getCreatedAt()); }
}
