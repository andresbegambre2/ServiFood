package com.servifood.application;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import com.servifood.domain.exception.DomainException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
public class InventoryConsumptionService {
    private final IngredientRepository ingredients;
    private final ProductRecipeRepository productRecipes;
    private final ExtraRecipeRepository extraRecipes;
    private final InventoryMovementRepository movements;
    public InventoryConsumptionService(IngredientRepository ingredients, ProductRecipeRepository productRecipes, ExtraRecipeRepository extraRecipes, InventoryMovementRepository movements) { this.ingredients = ingredients; this.productRecipes = productRecipes; this.extraRecipes = extraRecipes; this.movements = movements; }

    public void consume(CustomerOrder order) {
        if (order.getInventoryConsumedAt() != null) return;
        Map<Long, BigDecimal> required = new TreeMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) for (ProductRecipeIngredient line : productRecipes.findByProductIdOrderByIngredientNameAsc(item.getProduct().getId())) add(required, line.getIngredient().getId(), line.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity())));
            for (OrderItemExtra selected : item.getExtras()) if (selected.getExtra() != null) for (ExtraRecipeIngredient line : extraRecipes.findByExtraIdOrderByIngredientNameAsc(selected.getExtra().getId())) add(required, line.getIngredient().getId(), line.getQuantity().multiply(BigDecimal.valueOf(selected.getQuantity())));
        }
        Map<Long, Ingredient> locked = lock(required.keySet());
        required.forEach((id, quantity) -> { Ingredient ingredient = locked.get(id); if (!ingredient.isActive()) throw new DomainException(ingredient.getName() + " está inactivo"); if (ingredient.getStockCurrent().compareTo(quantity) < 0) throw new DomainException("Stock insuficiente de " + ingredient.getName()); });
        required.forEach((id, quantity) -> { Ingredient ingredient = locked.get(id); ingredient.adjust(quantity.negate()); movements.save(new InventoryMovement(ingredient, order, null, InventoryMovementType.CONSUMPTION, quantity.negate(), "Consumo del pedido " + order.getPublicNumber())); });
        order.markInventoryConsumed();
    }

    public void reverse(CustomerOrder order) {
        if (order.getInventoryConsumedAt() == null || order.getInventoryRevertedAt() != null) return;
        List<InventoryMovement> consumed = movements.findByOrderIdAndTypeOrderByIngredientIdAsc(order.getId(), InventoryMovementType.CONSUMPTION);
        Map<Long, Ingredient> locked = lock(consumed.stream().map(value -> value.getIngredient().getId()).distinct().toList());
        for (InventoryMovement movement : consumed) { BigDecimal quantity = movement.getQuantityDelta().abs(); Ingredient ingredient = locked.get(movement.getIngredient().getId()); ingredient.adjust(quantity); movements.save(new InventoryMovement(ingredient, order, null, InventoryMovementType.REVERSAL, quantity, "Reversión por cancelación del pedido " + order.getPublicNumber())); }
        order.markInventoryReverted();
    }

    private Map<Long, Ingredient> lock(Collection<Long> ids) { Map<Long, Ingredient> values = new LinkedHashMap<>(); ids.stream().sorted().forEach(id -> values.put(id, ingredients.findByIdForUpdate(id).orElseThrow())); return values; }
    private void add(Map<Long, BigDecimal> values, Long id, BigDecimal quantity) { values.merge(id, quantity.setScale(3), BigDecimal::add); }
}
