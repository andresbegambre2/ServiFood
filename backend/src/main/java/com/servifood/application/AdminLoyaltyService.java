package com.servifood.application;

import static com.servifood.presentation.rest.dto.LoyaltyDtos.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.*;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
public class AdminLoyaltyService {
    private final CustomerRepository customers; private final CustomerOrderRepository orders; private final LoyaltyPointMovementRepository movements;
    private final InternalUserRepository users; private final CouponRepository coupons; private final CouponRedemptionRepository redemptions;
    private final LoyaltySettingsRepository settings; private final InventoryAvailabilityService inventory;
    public AdminLoyaltyService(CustomerRepository customers, CustomerOrderRepository orders, LoyaltyPointMovementRepository movements,
            InternalUserRepository users, CouponRepository coupons, CouponRedemptionRepository redemptions,
            LoyaltySettingsRepository settings, InventoryAvailabilityService inventory) {
        this.customers = customers; this.orders = orders; this.movements = movements; this.users = users; this.coupons = coupons;
        this.redemptions = redemptions; this.settings = settings; this.inventory = inventory;
    }
    @Transactional(readOnly = true)
    public List<CustomerSummary> customers() { return orders.findCustomerSummaries().stream().map(value -> new CustomerSummary(
            value.getId(), value.getName(), value.getPhone(), value.getPoints(), value.getOrderCount(), value.getTotalSpent(), value.getLastOrderAt())).toList(); }
    @Transactional(readOnly = true)
    public CustomerProfile customer(Long id) {
        Customer customer = customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        List<CustomerOrder> history = orders.findByCustomerIdOrderByCreatedAtDesc(id);
        BigDecimal spent = history.stream().filter(order -> order.getStatus() == OrderStatus.DELIVERED).map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, ProductCounter> products = new HashMap<>();
        history.stream().filter(order -> order.getStatus() != OrderStatus.CANCELLED).flatMap(order -> order.getItems().stream()).filter(item -> item.getProduct() != null).forEach(item -> products.compute(item.getProduct().getId(), (key, current) -> current == null ? new ProductCounter(item.getProductNameSnapshot(), item.getQuantity()) : current.add(item.getQuantity())));
        return new CustomerProfile(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getPointsBalance(), history.size(), spent,
                history.isEmpty() ? null : history.get(0).getCreatedAt(), customer.getAddresses().stream().map(address -> new AddressView(address.getId(), address.getLabel(), address.getAddress(), address.getNeighborhood(), address.getReference(), address.isPrimaryAddress())).toList(),
                history.stream().map(this::orderView).toList(), products.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue().quantity(), a.getValue().quantity())).limit(5).map(entry -> new FrequentProduct(entry.getKey(), entry.getValue().name(), entry.getValue().quantity())).toList(),
                movements.findByCustomerIdOrderByCreatedAtDesc(id).stream().map(this::movement).toList());
    }
    @Transactional
    public CustomerProfile adjust(Long id, PointAdjustment request, Authentication authentication) {
        Customer customer = customers.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        customer.adjustPoints(request.points()); InternalUser user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        movements.save(new LoyaltyPointMovement(customer, null, user, LoyaltyMovementType.ADJUSTMENT, request.points(), request.reason())); return customer(id);
    }
    @Transactional(readOnly = true)
    public RepeatOrderResponse repeat(Long customerId, String publicNumber) {
        CustomerOrder order = orders.findByPublicNumber(publicNumber).filter(value -> value.getCustomer() != null && value.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Order", publicNumber));
        List<RepeatLine> lines = order.getItems().stream().map(item -> {
            Product product = item.getProduct();
            if (product == null || !product.isAvailable() || !inventory.canPrepare(product)) throw new CheckoutException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", item.getProductNameSnapshot() + " ya no está disponible.");
            List<RepeatExtra> extras = item.getExtras().stream().map(selected -> {
                Extra extra = selected.getExtra();
                if (extra == null || !extra.isAvailable() || !product.getAllowedExtras().contains(extra) || !inventory.canPrepare(extra)) throw new CheckoutException(HttpStatus.CONFLICT, "EXTRA_UNAVAILABLE", selected.getExtraNameSnapshot() + " ya no está disponible.");
                return new RepeatExtra(extra.getId(), extra.getName(), extra.getPrice());
            }).toList();
            return new RepeatLine(product.getId(), product.getSlug(), product.getName(), product.getImagePath(), product.getPrice(), item.getQuantity(), item.getNotes(), extras);
        }).toList();
        return new RepeatOrderResponse(lines);
    }
    @Transactional(readOnly = true) public List<CouponView> coupons() { return coupons.findAllByOrderByCreatedAtDesc().stream().map(this::coupon).toList(); }
    @Transactional public CouponView createCoupon(CouponRequest request) { if (coupons.findByCodeIgnoreCase(request.code()).isPresent()) throw new DomainException("El código ya existe"); return coupon(coupons.save(entity(request))); }
    @Transactional public CouponView updateCoupon(Long id, CouponRequest request) { Coupon coupon = coupons.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon", id)); coupons.findByCodeIgnoreCase(request.code()).filter(other -> !other.getId().equals(id)).ifPresent(other -> { throw new DomainException("El código ya existe"); }); coupon.update(request.code(), request.discountType(), request.discountValue(), request.startsAt(), request.endsAt(), request.minimumPurchase(), request.totalUsageLimit(), request.perCustomerUsageLimit(), request.active()); return coupon(coupon); }
    @Transactional(readOnly = true) public LoyaltySettingsView settings() { return setting(settings.findFirstByOrderByIdAsc().orElseThrow()); }
    @Transactional public LoyaltySettingsView updateSettings(LoyaltySettingsRequest request) { LoyaltySettings value = settings.findFirstByOrderByIdAsc().orElseThrow(); value.update(request.amountPerPoint(), request.minimumPointsToRedeem(), request.maximumRedemptionPercentage(), request.active()); return setting(value); }
    private CustomerOrderView orderView(CustomerOrder order) { return new CustomerOrderView(order.getPublicNumber(), order.getCreatedAt(), order.getStatus(), order.getTotal(), order.getDiscount(), order.getCouponCodeSnapshot(), order.getPointsRedeemed(), order.getPointsEarned()); }
    private PointMovementView movement(LoyaltyPointMovement value) { return new PointMovementView(value.getId(), value.getType(), value.getPointsDelta(), value.getBalanceAfter(), value.getReason(), value.getOrder() == null ? null : value.getOrder().getPublicNumber(), value.getCreatedBy() == null ? null : value.getCreatedBy().getName(), value.getCreatedAt()); }
    private Coupon entity(CouponRequest request) { return new Coupon(request.code(), request.discountType(), request.discountValue(), request.startsAt(), request.endsAt(), request.minimumPurchase(), request.totalUsageLimit(), request.perCustomerUsageLimit(), request.active()); }
    private CouponView coupon(Coupon value) { return new CouponView(value.getId(), value.getCode(), value.getDiscountType(), value.getDiscountValue(), value.getStartsAt(), value.getEndsAt(), value.getMinimumPurchase(), value.getTotalUsageLimit(), value.getPerCustomerUsageLimit(), redemptions.countByCouponIdAndReversedAtIsNull(value.getId()), value.isActive()); }
    private LoyaltySettingsView setting(LoyaltySettings value) { return new LoyaltySettingsView(value.getId(), value.getAmountPerPoint(), value.getMinimumPointsToRedeem(), value.getMaximumRedemptionPercentage(), value.isActive()); }
    private record ProductCounter(String name, long quantity) { ProductCounter add(long amount) { return new ProductCounter(name, quantity + amount); } }
}
