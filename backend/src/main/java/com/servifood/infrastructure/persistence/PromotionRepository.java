package com.servifood.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import com.servifood.domain.model.Promotion;
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByEndsAtAsc(Instant startsAt, Instant endsAt);
    List<Promotion> findAllByOrderByStartsAtDesc();
}
