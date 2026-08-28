package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.LoyaltySettings;
public interface LoyaltySettingsRepository extends JpaRepository<LoyaltySettings, Long> { Optional<LoyaltySettings> findFirstByOrderByIdAsc(); }
