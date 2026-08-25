package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.InternalUser;
public interface InternalUserRepository extends JpaRepository<InternalUser, Long> { Optional<InternalUser> findByEmailIgnoreCase(String email); }

