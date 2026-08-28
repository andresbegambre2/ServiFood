package com.servifood.application;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.DomainException;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
public class AdminOrderService {
    private final CustomerOrderRepository orders;
    private final PaymentRepository payments;
    private final InternalUserRepository users;
    private final BusinessSettingsRepository settings;
    private final ReceiptStorage receipts;
    private final InventoryConsumptionService inventory;
    private final IngredientRepository ingredients;
    private final LoyaltyService loyalty;

    public AdminOrderService(CustomerOrderRepository orders, PaymentRepository payments, InternalUserRepository users,
            BusinessSettingsRepository settings, ReceiptStorage receipts, InventoryConsumptionService inventory, IngredientRepository ingredients,
            LoyaltyService loyalty) {
        this.orders = orders; this.payments = payments; this.users = users; this.settings = settings; this.receipts = receipts; this.inventory = inventory; this.ingredients = ingredients; this.loyalty = loyalty;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        ZoneId zone = ZoneId.of(settings.findFirstByOrderByIdAsc().map(BusinessSettings::getTimeZone).orElse("America/Bogota"));
        LocalDate today = LocalDate.now(zone);
        List<CustomerOrder> todayOrders = orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> LocalDate.ofInstant(order.getCreatedAt(), zone).equals(today)).toList();
        List<CustomerOrder> effective = todayOrders.stream().filter(order -> order.getStatus() != OrderStatus.CANCELLED).toList();
        BigDecimal sales = effective.stream().map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = effective.isEmpty() ? BigDecimal.ZERO : sales.divide(BigDecimal.valueOf(effective.size()), 2, RoundingMode.HALF_UP);
        Map<String, Long> quantities = effective.stream().flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getProductNameSnapshot, Collectors.summingLong(OrderItem::getQuantity)));
        List<TopProduct> top = quantities.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .map(entry -> new TopProduct(entry.getKey(), entry.getValue())).toList();
        long underReview = payments.findByStatusOrderByCreatedAtDesc(PaymentStatus.UNDER_REVIEW).size();
        return new Dashboard(sales, todayOrders.size(), count(todayOrders, OrderStatus.NEW), count(todayOrders, OrderStatus.PREPARING),
                underReview, ingredients.countLowStock(), ingredients.countOutOfStock(), average, todayOrders.stream().limit(8).map(this::summary).toList(), top);
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> list(OrderStatus status, PaymentMethod method, DeliveryType deliveryType, LocalDate date, String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> deliveryType == null || order.getDeliveryType() == deliveryType)
                .filter(order -> date == null || LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).equals(date))
                .filter(order -> term.isEmpty() || order.getPublicNumber().toLowerCase(Locale.ROOT).contains(term) || order.getCustomerNameSnapshot().toLowerCase(Locale.ROOT).contains(term))
                .filter(order -> method == null || payment(order).map(Payment::getMethod).orElse(null) == method)
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public OrderDetail detail(String publicNumber) { return detailOf(order(publicNumber)); }

    @Transactional
    public OrderDetail changeStatus(String publicNumber, OrderStatus target, String reason) {
        CustomerOrder order = orders.findLockedByPublicNumber(publicNumber).orElseThrow(() -> new ResourceNotFoundException("Order", publicNumber));
        switch (target) {
            case CONFIRMED -> order.confirm();
            case PREPARING -> { order.startPreparation(); inventory.consume(order); }
            case READY -> order.markReady();
            case ON_THE_WAY -> order.markOnTheWay();
            case DELIVERED -> { order.deliver(); loyalty.award(order); }
            case CANCELLED -> { order.cancel(reason); inventory.reverse(order); loyalty.reverse(order); }
            default -> throw new DomainException("Transición de estado no permitida");
        }
        return detailOf(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<PaymentQueueItem> paymentQueue(PaymentStatus status) {
        List<Payment> values = status == null ? payments.findAll() : payments.findByStatusOrderByCreatedAtDesc(status);
        return values.stream().sorted(Comparator.comparing(Payment::getCreatedAt).reversed()).map(payment -> new PaymentQueueItem(
                payment.getOrder().getPublicNumber(), payment.getOrder().getCustomerNameSnapshot(), payment.getCreatedAt(), payment.getAmount(),
                payment.getMethod(), payment.getStatus(), payment.getReceiptPath() != null)).toList();
    }

    @Transactional
    public OrderDetail approvePayment(String publicNumber, Authentication authentication) {
        Payment payment = reviewablePayment(publicNumber); payment.approve(currentUser(authentication)); payments.save(payment); return detailOf(payment.getOrder());
    }

    @Transactional
    public OrderDetail rejectPayment(String publicNumber, String reason, Authentication authentication) {
        Payment payment = reviewablePayment(publicNumber); payment.reject(currentUser(authentication), reason); payments.save(payment); return detailOf(payment.getOrder());
    }

    @Transactional(readOnly = true)
    public ReceiptStorage.StoredFile receipt(String publicNumber) {
        Payment payment = payment(order(publicNumber)).orElseThrow(() -> new ResourceNotFoundException("Payment", publicNumber));
        return receipts.read(payment.getReceiptPath());
    }

    private Payment reviewablePayment(String publicNumber) {
        Payment payment = payment(order(publicNumber)).orElseThrow(() -> new ResourceNotFoundException("Payment", publicNumber));
        if (payment.getStatus() != PaymentStatus.UNDER_REVIEW) throw new DomainException("Solo los pagos en revisión pueden aprobarse o rechazarse");
        return payment;
    }
    private InternalUser currentUser(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(); }
    private long count(List<CustomerOrder> values, OrderStatus status) { return values.stream().filter(order -> order.getStatus() == status).count(); }
    private CustomerOrder order(String number) { return orders.findByPublicNumber(number).orElseThrow(() -> new ResourceNotFoundException("Order", number)); }
    private Optional<Payment> payment(CustomerOrder order) { return payments.findFirstByOrderId(order.getId()); }
    private OrderSummary summary(CustomerOrder order) {
        Payment payment = payment(order).orElse(null);
        return new OrderSummary(order.getPublicNumber(), order.getCustomerNameSnapshot(), order.getCreatedAt(), order.getDeliveryType(), order.getTotal(),
                payment == null ? null : payment.getMethod(), payment == null ? null : payment.getStatus(), order.getStatus());
    }
    private OrderDetail detailOf(CustomerOrder order) {
        Payment payment = payment(order).orElse(null);
        List<OrderItemView> items = order.getItems().stream().map(item -> new OrderItemView(item.getProductNameSnapshot(), item.getUnitPriceSnapshot(), item.getQuantity(), item.getNotes(), item.getSubtotal(),
                item.getExtras().stream().map(extra -> new OrderExtraView(extra.getExtraNameSnapshot(), extra.getUnitPriceSnapshot(), extra.getQuantity(), extra.getSubtotal())).toList())).toList();
        PaymentView paymentView = payment == null ? null : new PaymentView(payment.getId(), payment.getMethod(), payment.getStatus(), payment.getAmount(), payment.getCashTendered(), payment.getReceiptPath() != null,
                payment.getReviewedBy() == null ? null : payment.getReviewedBy().getName(), payment.getReviewedAt(), payment.getRejectionReason());
        return new OrderDetail(order.getPublicNumber(), order.getCustomerNameSnapshot(), order.getCustomerPhoneSnapshot(), order.getCustomerEmailSnapshot(), order.getDeliveryAddressSnapshot(), order.getDeliveryType(),
                order.getSubtotal(), order.getDiscount(), order.getDeliveryFee(), order.getTotal(), order.getStatus(), order.getCreatedAt(), items, paymentView,
                new OrderTimeline(order.getCreatedAt(), order.getConfirmedAt(), order.getPreparedAt(), order.getReadyAt(), order.getOnTheWayAt(), order.getDeliveredAt(), order.getCancelledAt(), order.getCancellationReason()));
    }
}
