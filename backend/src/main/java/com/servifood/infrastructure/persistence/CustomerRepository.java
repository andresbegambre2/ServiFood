package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Customer;
public interface CustomerRepository extends JpaRepository<Customer, Long> { Optional<Customer> findFirstByPhone(String phone); }
