package com.servifood.infrastructure.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.*;
public interface LoyaltyPointMovementRepository extends JpaRepository<LoyaltyPointMovement, Long> {
    List<LoyaltyPointMovement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<LoyaltyPointMovement> findByOrderIdAndType(Long orderId, LoyaltyMovementType type);
}
