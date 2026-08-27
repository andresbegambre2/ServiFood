package com.servifood.infrastructure.persistence;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.servifood.domain.model.CustomerOrder;
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByPublicNumber(String publicNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from CustomerOrder o where o.publicNumber = :publicNumber")
    Optional<CustomerOrder> findLockedByPublicNumber(@Param("publicNumber") String publicNumber);
    Optional<CustomerOrder> findByClientRequestId(String clientRequestId);
    boolean existsByPublicNumber(String publicNumber);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
