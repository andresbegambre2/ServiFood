package com.servifood.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
@Transactional(readOnly = true)
public class InventoryAvailabilityService {
    private final ProductRecipeRepository productRecipes;
    private final ExtraRecipeRepository extraRecipes;
    public InventoryAvailabilityService(ProductRecipeRepository productRecipes, ExtraRecipeRepository extraRecipes) { this.productRecipes = productRecipes; this.extraRecipes = extraRecipes; }
    public boolean canPrepare(Product product) { return product.isAvailable() && productRecipes.findByProductIdOrderByIngredientNameAsc(product.getId()).stream().allMatch(this::available); }
    public boolean canPrepare(Extra extra) { return extra.isAvailable() && extraRecipes.findByExtraIdOrderByIngredientNameAsc(extra.getId()).stream().allMatch(this::available); }
    private boolean available(ProductRecipeIngredient line) { return line.getIngredient().isActive() && line.getIngredient().getStockCurrent().compareTo(line.getQuantity()) >= 0; }
    private boolean available(ExtraRecipeIngredient line) { return line.getIngredient().isActive() && line.getIngredient().getStockCurrent().compareTo(line.getQuantity()) >= 0; }
}
