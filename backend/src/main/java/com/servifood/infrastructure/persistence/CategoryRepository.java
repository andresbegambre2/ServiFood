package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Category;
public interface CategoryRepository extends JpaRepository<Category, Long> { Optional<Category> findBySlug(String slug); }
