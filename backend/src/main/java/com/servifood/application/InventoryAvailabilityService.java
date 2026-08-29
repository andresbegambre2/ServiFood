package com.servifood.application;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public Map<Long, Boolean> canPrepareProducts(Collection<Product> products) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        products.forEach(product -> result.put(product.getId(), product.isAvailable()));
        if (!result.isEmpty()) productRecipes.findByProductIdInOrderByIngredientNameAsc(result.keySet()).forEach(line ->
                result.computeIfPresent(line.getProduct().getId(), (id, current) -> current && available(line)));
        return result;
    }
    public Map<Long, Boolean> canPrepareExtras(Collection<Extra> extras) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        extras.forEach(extra -> result.put(extra.getId(), extra.isAvailable()));
        if (!result.isEmpty()) extraRecipes.findByExtraIdInOrderByIngredientNameAsc(result.keySet()).forEach(line ->
                result.computeIfPresent(line.getExtra().getId(), (id, current) -> current && available(line)));
        return result;
    }
    private boolean available(ProductRecipeIngredient line) { return line.getIngredient().isActive() && line.getIngredient().getStockCurrent().compareTo(line.getQuantity()) >= 0; }
    private boolean available(ExtraRecipeIngredient line) { return line.getIngredient().isActive() && line.getIngredient().getStockCurrent().compareTo(line.getQuantity()) >= 0; }
}
