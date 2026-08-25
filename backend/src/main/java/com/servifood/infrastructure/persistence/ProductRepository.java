package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Product;
public interface ProductRepository extends JpaRepository<Product, Long> { Optional<Product> findBySlug(String slug); }

