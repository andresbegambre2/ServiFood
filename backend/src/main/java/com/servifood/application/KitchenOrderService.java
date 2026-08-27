package com.servifood.application;

import static com.servifood.presentation.rest.dto.KitchenDtos.*;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.DomainException;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.CustomerOrderRepository;
import com.servifood.infrastructure.persistence.PaymentRepository;

@Service
public class KitchenOrderService {
    private final CustomerOrderRepository orders;
    private final PaymentRepository payments;

    public KitchenOrderService(CustomerOrderRepository orders, PaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public List<KitchenOrder> activeOrders() {
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::hasKitchenStatus)
                .filter(this::isPaymentEligible)
                .sorted(Comparator.comparing(CustomerOrder::getCreatedAt))
                .map(this::view)
                .toList();
    }

    @Transactional
    public KitchenOrder transition(String publicNumber, KitchenStage target) {
        CustomerOrder order = orders.findByPublicNumber(publicNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", publicNumber));
        if (!isPaymentEligible(order)) throw new DomainException("El pedido todavía no está habilitado para cocina");
        if (order.getStatus() == OrderStatus.CONFIRMED && target == KitchenStage.PREPARING) {
            order.startPreparation();
        } else if (order.getStatus() == OrderStatus.PREPARING && target == KitchenStage.READY) {
            order.markReady();
        } else {
            throw new DomainException("Transición de cocina no permitida");
        }
        return view(orders.save(order));
    }

    private boolean hasKitchenStatus(CustomerOrder order) {
        return order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.READY;
    }

    private boolean isPaymentEligible(CustomerOrder order) {
        Payment payment = payments.findFirstByOrderId(order.getId()).orElse(null);
        if (payment == null) return false;
        return payment.getMethod() != PaymentMethod.TRANSFER || payment.getStatus() == PaymentStatus.APPROVED;
    }

    private KitchenOrder view(CustomerOrder order) {
        KitchenStage stage = switch (order.getStatus()) {
            case CONFIRMED -> KitchenStage.NEW;
            case PREPARING -> KitchenStage.PREPARING;
            case READY -> KitchenStage.READY;
            default -> throw new DomainException("El pedido no pertenece al tablero de cocina");
        };
        List<KitchenItem> items = order.getItems().stream().map(item -> new KitchenItem(
                item.getProductNameSnapshot(), item.getQuantity(), item.getNotes(),
                item.getExtras().stream().map(extra -> new KitchenExtra(extra.getExtraNameSnapshot(), extra.getQuantity())).toList()
        )).toList();
        return new KitchenOrder(order.getPublicNumber(), order.getCreatedAt(), stage,
                order.getDeliveryType(), order.getNotes(), items);
    }
}
