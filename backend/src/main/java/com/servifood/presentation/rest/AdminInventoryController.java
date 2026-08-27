package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.InventoryDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.AdminInventoryService;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {
    private final AdminInventoryService service;
    public AdminInventoryController(AdminInventoryService service) { this.service = service; }
    @GetMapping Overview overview() { return service.overview(); }
    @PostMapping("/ingredients") IngredientView create(@Valid @RequestBody CreateIngredient request, Authentication authentication) { return service.create(request, authentication); }
    @PutMapping("/ingredients/{id}") IngredientView update(@PathVariable Long id, @Valid @RequestBody UpdateIngredient request) { return service.update(id, request); }
    @PostMapping("/ingredients/{id}/adjustments") IngredientView adjust(@PathVariable Long id, @Valid @RequestBody StockAdjustment request, Authentication authentication) { return service.adjust(id, request, authentication); }
    @PutMapping("/recipes/products/{id}") RecipeView productRecipe(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) { return service.replaceProductRecipe(id, request); }
    @PutMapping("/recipes/extras/{id}") RecipeView extraRecipe(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) { return service.replaceExtraRecipe(id, request); }
}
