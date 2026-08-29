package com.servifood.infrastructure.persistence;
import java.util.Optional;
import java.util.List;
import com.servifood.domain.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.servifood.domain.model.Payment;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrderId(Long orderId);
    @EntityGraph(attributePaths = "order") List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    @EntityGraph(attributePaths = "order") List<Payment> findAllByOrderByCreatedAtDesc();
}
