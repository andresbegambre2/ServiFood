package com.servifood.infrastructure.persistence;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.*;
import com.servifood.domain.model.ProductRecipeIngredient;

public interface ProductRecipeRepository extends JpaRepository<ProductRecipeIngredient, Long> {
    @EntityGraph(attributePaths = "ingredient") List<ProductRecipeIngredient> findByProductIdOrderByIngredientNameAsc(Long productId);
    @EntityGraph(attributePaths = "ingredient") List<ProductRecipeIngredient> findByProductIdInOrderByIngredientNameAsc(Collection<Long> productIds);
    void deleteByProductId(Long productId);
}
