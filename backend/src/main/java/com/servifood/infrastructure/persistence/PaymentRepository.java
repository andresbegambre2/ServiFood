package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Payment;
public interface PaymentRepository extends JpaRepository<Payment, Long> { Optional<Payment> findFirstByOrderId(Long orderId); }
