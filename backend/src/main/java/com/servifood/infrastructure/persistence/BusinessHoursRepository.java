package com.servifood.infrastructure.persistence;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.BusinessHours;
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> { List<BusinessHours> findByDayOfWeekOrderBySlotNumber(DayOfWeek day); }
