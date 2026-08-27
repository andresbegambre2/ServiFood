package com.servifood.infrastructure.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.Extra;
public interface ExtraRepository extends JpaRepository<Extra, Long> { List<Extra> findAllByOrderByNameAsc(); }
