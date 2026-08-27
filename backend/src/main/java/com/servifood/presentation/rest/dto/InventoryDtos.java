package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.servifood.domain.model.*;

public final class InventoryDtos {
    private InventoryDtos() {}
    public record Overview(long trackedIngredients, long lowStockCount, long outOfStockCount, List<IngredientView> ingredients,
            List<RecipeView> productRecipes, List<RecipeView> extraRecipes, List<MovementView> recentMovements) {}
    public record IngredientView(Long id, String name, IngredientUnit unit, BigDecimal stockCurrent, BigDecimal stockMinimum,
            BigDecimal unitCost, boolean active, String stockStatus) {}
    public record CreateIngredient(@NotBlank @Size(max = 150) String name, @NotNull IngredientUnit unit,
            @NotNull @DecimalMin("0.000") BigDecimal initialStock, @NotNull @DecimalMin("0.000") BigDecimal stockMinimum,
            @DecimalMin("0.0000") BigDecimal unitCost, boolean active) {}
    public record UpdateIngredient(@NotBlank @Size(max = 150) String name, @NotNull IngredientUnit unit,
            @NotNull @DecimalMin("0.000") BigDecimal stockMinimum, @DecimalMin("0.0000") BigDecimal unitCost, boolean active) {}
    public record StockAdjustment(@NotNull InventoryMovementType type, @NotNull BigDecimal quantity,
            @NotBlank @Size(max = 500) String reason) {
        @AssertTrue(message = "Solo se permiten entradas o ajustes manuales") public boolean isManualType() { return type == InventoryMovementType.ENTRY || type == InventoryMovementType.ADJUSTMENT; }
        @AssertTrue(message = "La cantidad de una entrada debe ser positiva") public boolean isEntryPositive() { return quantity == null || type != InventoryMovementType.ENTRY || quantity.signum() > 0; }
        @AssertTrue(message = "La cantidad del ajuste no puede ser cero") public boolean isNotZero() { return quantity == null || quantity.signum() != 0; }
    }
    public record RecipeLine(Long ingredientId, String ingredientName, IngredientUnit unit, BigDecimal quantity) {}
    public record RecipeView(Long targetId, String targetName, boolean effectiveAvailable, List<RecipeLine> ingredients) {}
    public record RecipeLineRequest(@NotNull Long ingredientId, @NotNull @DecimalMin("0.001") BigDecimal quantity) {}
    public record RecipeRequest(List<@Valid RecipeLineRequest> ingredients) { public RecipeRequest { ingredients = ingredients == null ? List.of() : List.copyOf(ingredients); } }
    public record MovementView(Long id, Long ingredientId, String ingredientName, InventoryMovementType type,
            BigDecimal quantityDelta, BigDecimal balanceAfter, String reason, String orderNumber, String createdBy, Instant createdAt) {}
}
