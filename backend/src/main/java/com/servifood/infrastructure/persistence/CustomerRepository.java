package com.servifood.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Customer;
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
