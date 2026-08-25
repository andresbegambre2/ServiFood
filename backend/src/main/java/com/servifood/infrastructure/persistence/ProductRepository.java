package com.servifood.infrastructure.persistence;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.servifood.domain.model.Product;
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category", "allowedExtras"}) Optional<Product> findBySlugAndAvailableTrue(String slug);
    @EntityGraph(attributePaths = "category") List<Product> findByAvailableTrueOrderByFeaturedDescNameAsc();
    @EntityGraph(attributePaths = "category") List<Product> findByAvailableTrueAndFeaturedTrueOrderByNameAsc();
    Optional<Product> findBySlug(String slug);
}
