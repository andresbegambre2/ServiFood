package com.servifood.infrastructure.persistence;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.servifood.domain.model.Customer;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findFirstByPhone(String phone);
    List<Customer> findAllByOrderByNameAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Customer c where c.id = :id") Optional<Customer> findLockedById(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Customer c where c.phone = :phone order by c.id") List<Customer> findLockedByPhone(@Param("phone") String phone);
}
