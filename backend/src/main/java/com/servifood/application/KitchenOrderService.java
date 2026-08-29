package com.servifood.application;

import static com.servifood.presentation.rest.dto.KitchenDtos.*;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.DomainException;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.CustomerOrder;
import com.servifood.domain.model.OrderStatus;
import com.servifood.infrastructure.persistence.CustomerOrderRepository;

@Service
public class KitchenOrderService {
    private static final EnumSet<OrderStatus> VISIBLE = EnumSet.of(
            OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY);
    private final CustomerOrderRepository orders;
    private final InventoryConsumptionService inventory;

    public KitchenOrderService(CustomerOrderRepository orders, InventoryConsumptionService inventory) {
        this.orders = orders; this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public List<KitchenOrder> list() {
        return orders.findByStatusInOrderByCreatedAtAsc(VISIBLE).stream().map(this::view).toList();
    }

    @Transactional
    public KitchenOrder changeStatus(String publicNumber, OrderStatus target) {
        CustomerOrder order = orders.findLockedByPublicNumber(publicNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", publicNumber));
        if (target == OrderStatus.PREPARING) {
            if (order.getStatus() == OrderStatus.NEW) order.confirm();
            order.startPreparation();
            inventory.consume(order);
        } else if (target == OrderStatus.READY) {
            order.markReady();
        } else {
            throw new DomainException("Cocina solo puede iniciar o finalizar la preparación");
        }
        return view(orders.save(order));
    }

    private KitchenOrder view(CustomerOrder order) {
        return new KitchenOrder(order.getPublicNumber(), order.getCreatedAt(), order.getDeliveryType(), order.getStatus(),
                order.getItems().stream().map(item -> new KitchenItem(item.getProductNameSnapshot(), item.getQuantity(), item.getNotes(),
                        item.getExtras().stream().map(extra -> extra.getQuantity() + " × " + extra.getExtraNameSnapshot()).toList())).toList());
    }
}
