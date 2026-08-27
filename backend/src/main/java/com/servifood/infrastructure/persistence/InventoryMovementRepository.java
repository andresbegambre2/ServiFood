package com.servifood.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import com.servifood.domain.model.InventoryMovement;
import com.servifood.domain.model.InventoryMovementType;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    @EntityGraph(attributePaths = {"ingredient", "order", "createdBy"}) List<InventoryMovement> findTop200ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"ingredient", "order", "createdBy"}) List<InventoryMovement> findTop200ByIngredientIdOrderByCreatedAtDesc(Long ingredientId);
    @EntityGraph(attributePaths = "ingredient") List<InventoryMovement> findByOrderIdAndTypeOrderByIngredientIdAsc(Long orderId, InventoryMovementType type);
}
