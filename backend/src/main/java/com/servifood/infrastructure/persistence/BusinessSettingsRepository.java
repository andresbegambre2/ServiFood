package com.servifood.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.servifood.domain.model.BusinessSettings;
public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> { Optional<BusinessSettings> findFirstByOrderByIdAsc(); }
