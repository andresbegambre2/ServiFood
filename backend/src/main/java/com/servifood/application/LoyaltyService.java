package com.servifood.application;

import java.math.*;
import java.time.Clock;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.*;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;
import com.servifood.presentation.rest.dto.LoyaltyDtos.LoyaltyQuote;

@Service
public class LoyaltyService {
    private final LoyaltySettingsRepository settings; private final CouponRepository coupons; private final CouponRedemptionRepository redemptions;
    private final LoyaltyPointMovementRepository movements; private final CustomerRepository customers; private final Clock clock = Clock.systemUTC();
    public LoyaltyService(LoyaltySettingsRepository settings, CouponRepository coupons, CouponRedemptionRepository redemptions,
            LoyaltyPointMovementRepository movements, CustomerRepository customers) { this.settings = settings; this.coupons = coupons; this.redemptions = redemptions; this.movements = movements; this.customers = customers; }

    @Transactional(readOnly = true)
    public Pricing preview(Customer customer, BigDecimal eligibleAmount, String couponCode, Integer requestedPoints) {
        return pricing(customer, eligibleAmount, couponCode, requestedPoints, false);
    }
    public Pricing reservePreview(Customer customer, BigDecimal eligibleAmount, String couponCode, Integer requestedPoints) {
        return pricing(customer, eligibleAmount, couponCode, requestedPoints, true);
    }
    private Pricing pricing(Customer customer, BigDecimal eligibleAmount, String couponCode, Integer requestedPoints, boolean lockCoupon) {
        LoyaltySettings config = settings.findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("Loyalty settings", "default"));
        BigDecimal couponDiscount = money(BigDecimal.ZERO); Coupon coupon = null; String normalized = normalizeCoupon(couponCode);
        if (normalized != null) {
            coupon = (lockCoupon ? coupons.findLockedByCode(normalized) : coupons.findByCodeIgnoreCase(normalized))
                    .orElseThrow(() -> invalid("COUPON_INVALID", "El cupón no existe."));
            validateCoupon(coupon, customer, eligibleAmount);
            couponDiscount = discount(coupon, eligibleAmount);
        }
        int available = customer == null ? 0 : customer.getPointsBalance(); int requested = requestedPoints == null ? 0 : requestedPoints;
        if (requested < 0) throw invalid("POINTS_INVALID", "Los puntos a redimir no son válidos.");
        BigDecimal afterCoupon = eligibleAmount.subtract(couponDiscount).max(BigDecimal.ZERO); int redeemed = 0; BigDecimal pointsDiscount = money(BigDecimal.ZERO);
        if (requested > 0) {
            if (!config.isActive()) throw invalid("LOYALTY_INACTIVE", "La redención de puntos no está disponible.");
            if (customer == null) throw invalid("CUSTOMER_REQUIRED", "Escribe un teléfono registrado para usar puntos.");
            if (requested < config.getMinimumPointsToRedeem()) throw invalid("POINTS_MINIMUM", "Debes redimir al menos " + config.getMinimumPointsToRedeem() + " puntos.");
            BigDecimal cap = afterCoupon.multiply(BigDecimal.valueOf(config.getMaximumRedemptionPercentage())).divide(new BigDecimal("100"), 2, RoundingMode.DOWN);
            int maximumByAmount = cap.divide(config.getAmountPerPoint(), 0, RoundingMode.DOWN).intValue();
            redeemed = Math.min(requested, Math.min(available, maximumByAmount));
            if (redeemed < config.getMinimumPointsToRedeem()) throw invalid("POINTS_NOT_APPLICABLE", "No hay suficientes puntos aplicables para este pedido.");
            pointsDiscount = money(config.getAmountPerPoint().multiply(BigDecimal.valueOf(redeemed)).min(afterCoupon));
        }
        int toEarn = config.isActive() ? afterCoupon.subtract(pointsDiscount).max(BigDecimal.ZERO).divide(config.getAmountPerPoint(), 0, RoundingMode.DOWN).intValue() : 0;
        LoyaltyQuote quote = new LoyaltyQuote(config.isActive(), available, redeemed, pointsDiscount, coupon == null ? null : coupon.getCode(), couponDiscount,
                toEarn, config.getMinimumPointsToRedeem(), config.getMaximumRedemptionPercentage(), config.getAmountPerPoint());
        return new Pricing(coupon, couponDiscount, redeemed, pointsDiscount, quote);
    }

    public void reserve(CustomerOrder order, Customer customer, Pricing pricing) {
        if (pricing.pointsRedeemed() > 0) { customer.redeemPoints(pricing.pointsRedeemed()); movements.save(new LoyaltyPointMovement(customer, order, null, LoyaltyMovementType.REDEEM, -pricing.pointsRedeemed(), "Redención en pedido " + order.getPublicNumber())); }
        if (pricing.coupon() != null) redemptions.save(new CouponRedemption(pricing.coupon(), customer, order, pricing.couponDiscount()));
    }
    @Transactional(readOnly = true)
    public LoyaltyQuote orderQuote(CustomerOrder order) {
        LoyaltySettings config = settings.findFirstByOrderByIdAsc().orElseThrow();
        int available = order.getCustomer() == null ? 0 : customers.findById(order.getCustomer().getId()).map(Customer::getPointsBalance).orElse(0);
        int earned = order.getLoyaltyAwardedAt() == null ? order.getSubtotal().subtract(order.getDiscount()).max(BigDecimal.ZERO).divide(config.getAmountPerPoint(), 0, RoundingMode.DOWN).intValue() : order.getPointsEarned();
        return new LoyaltyQuote(config.isActive(), available, order.getPointsRedeemed(), order.getPointsDiscount(), order.getCouponCodeSnapshot(),
                order.getCouponDiscount(), earned, config.getMinimumPointsToRedeem(), config.getMaximumRedemptionPercentage(), config.getAmountPerPoint());
    }
    public void award(CustomerOrder order) {
        if (order.getLoyaltyAwardedAt() != null || order.getCustomer() == null) return;
        LoyaltySettings config = settings.findFirstByOrderByIdAsc().orElseThrow(); if (!config.isActive()) { order.markLoyaltyAwarded(0); return; }
        Customer customer = customers.findLockedById(order.getCustomer().getId()).orElseThrow();
        int points = order.getSubtotal().subtract(order.getDiscount()).max(BigDecimal.ZERO).divide(config.getAmountPerPoint(), 0, RoundingMode.DOWN).intValue();
        if (points > 0) { customer.addPoints(points); movements.save(new LoyaltyPointMovement(customer, order, null, LoyaltyMovementType.EARN, points, "Puntos por pedido entregado " + order.getPublicNumber())); }
        order.markLoyaltyAwarded(points);
    }
    public void reverse(CustomerOrder order) {
        if (order.getLoyaltyReversedAt() != null || order.getCustomer() == null) return;
        Customer customer = customers.findLockedById(order.getCustomer().getId()).orElseThrow();
        if (order.getPointsRedeemed() > 0 && movements.findByOrderIdAndType(order.getId(), LoyaltyMovementType.REVERSAL_REDEEM).isEmpty()) {
            customer.addPoints(order.getPointsRedeemed()); movements.save(new LoyaltyPointMovement(customer, order, null, LoyaltyMovementType.REVERSAL_REDEEM, order.getPointsRedeemed(), "Devolución de puntos por cancelación " + order.getPublicNumber()));
        }
        if (order.getPointsEarned() > 0 && movements.findByOrderIdAndType(order.getId(), LoyaltyMovementType.REVERSAL_EARN).isEmpty()) {
            customer.adjustPoints(-order.getPointsEarned()); movements.save(new LoyaltyPointMovement(customer, order, null, LoyaltyMovementType.REVERSAL_EARN, -order.getPointsEarned(), "Corrección de puntos por reversión " + order.getPublicNumber()));
        }
        redemptions.findByOrderId(order.getId()).ifPresent(CouponRedemption::reverse); order.markLoyaltyReversed();
    }
    private void validateCoupon(Coupon coupon, Customer customer, BigDecimal subtotal) {
        var now = clock.instant();
        if (!coupon.isActive() || now.isBefore(coupon.getStartsAt()) || !now.isBefore(coupon.getEndsAt())) throw invalid("COUPON_INACTIVE", "El cupón no está vigente.");
        if (subtotal.compareTo(coupon.getMinimumPurchase()) < 0) throw invalid("COUPON_MINIMUM", "El pedido no alcanza la compra mínima del cupón.");
        if (coupon.getTotalUsageLimit() != null && redemptions.countByCouponIdAndReversedAtIsNull(coupon.getId()) >= coupon.getTotalUsageLimit()) throw invalid("COUPON_LIMIT", "El cupón agotó sus usos.");
        if (coupon.getPerCustomerUsageLimit() != null && customer != null && redemptions.countByCouponIdAndCustomerIdAndReversedAtIsNull(coupon.getId(), customer.getId()) >= coupon.getPerCustomerUsageLimit()) throw invalid("COUPON_CUSTOMER_LIMIT", "Ya alcanzaste el límite de uso de este cupón.");
    }
    private BigDecimal discount(Coupon coupon, BigDecimal amount) { BigDecimal value = coupon.getDiscountType() == DiscountType.PERCENTAGE ? amount.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : coupon.getDiscountValue(); return money(value.min(amount)); }
    private String normalizeCoupon(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private CheckoutException invalid(String code, String message) { return new CheckoutException(HttpStatus.CONFLICT, code, message); }
    public record Pricing(Coupon coupon, BigDecimal couponDiscount, int pointsRedeemed, BigDecimal pointsDiscount, LoyaltyQuote quote) {}
}
