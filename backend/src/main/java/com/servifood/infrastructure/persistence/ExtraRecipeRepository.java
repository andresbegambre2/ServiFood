package com.servifood.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import com.servifood.domain.model.ExtraRecipeIngredient;

public interface ExtraRecipeRepository extends JpaRepository<ExtraRecipeIngredient, Long> {
    @EntityGraph(attributePaths = "ingredient") List<ExtraRecipeIngredient> findByExtraIdOrderByIngredientNameAsc(Long extraId);
    void deleteByExtraId(Long extraId);
}
