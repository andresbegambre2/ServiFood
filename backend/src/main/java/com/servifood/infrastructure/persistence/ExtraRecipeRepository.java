package com.servifood.infrastructure.persistence;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.*;
import com.servifood.domain.model.ExtraRecipeIngredient;

public interface ExtraRecipeRepository extends JpaRepository<ExtraRecipeIngredient, Long> {
    @EntityGraph(attributePaths = "ingredient") List<ExtraRecipeIngredient> findByExtraIdOrderByIngredientNameAsc(Long extraId);
    @EntityGraph(attributePaths = "ingredient") List<ExtraRecipeIngredient> findByExtraIdInOrderByIngredientNameAsc(Collection<Long> extraIds);
    void deleteByExtraId(Long extraId);
}
