package com.servifood.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.servifood.domain.model.CouponRedemption;
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCouponIdAndReversedAtIsNull(Long couponId);
    long countByCouponIdAndCustomerIdAndReversedAtIsNull(Long couponId, Long customerId);
    Optional<CouponRedemption> findByOrderId(Long orderId);
}
