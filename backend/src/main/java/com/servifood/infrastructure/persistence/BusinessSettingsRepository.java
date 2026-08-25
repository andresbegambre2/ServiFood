package com.servifood.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.BusinessSettings;
public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {}
