package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.CustomerOrder;
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> { Optional<CustomerOrder> findByPublicNumber(String publicNumber); }
