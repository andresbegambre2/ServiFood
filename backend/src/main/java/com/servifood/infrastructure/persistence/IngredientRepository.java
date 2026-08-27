package com.servifood.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.servifood.domain.model.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByOrderByNameAsc();
    Optional<Ingredient> findByNameIgnoreCase(String name);
    @Query("select count(i) from Ingredient i where i.active = true and i.stockCurrent = 0") long countOutOfStock();
    @Query("select count(i) from Ingredient i where i.active = true and i.stockCurrent > 0 and i.stockCurrent <= i.stockMinimum") long countLowStock();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Ingredient i where i.id = :id") Optional<Ingredient> findByIdForUpdate(@Param("id") Long id);
}
