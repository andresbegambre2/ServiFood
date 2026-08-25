package com.servifood.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Promotion;
public interface PromotionRepository extends JpaRepository<Promotion, Long> {}
