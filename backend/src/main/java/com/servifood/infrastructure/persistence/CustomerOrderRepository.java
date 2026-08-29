package com.servifood.infrastructure.persistence;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.servifood.domain.model.CustomerOrder;
import com.servifood.domain.model.OrderStatus;
import com.servifood.domain.model.DeliveryType;
import com.servifood.domain.model.PaymentMethod;
import com.servifood.domain.model.PaymentStatus;
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByPublicNumber(String publicNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from CustomerOrder o where o.publicNumber = :publicNumber")
    Optional<CustomerOrder> findLockedByPublicNumber(@Param("publicNumber") String publicNumber);
    Optional<CustomerOrder> findByClientRequestId(String clientRequestId);
    boolean existsByPublicNumber(String publicNumber);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "items")
    List<CustomerOrder> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("""
            select o.publicNumber as publicNumber, o.customerNameSnapshot as customerName,
                   o.createdAt as createdAt, o.deliveryType as deliveryType, o.total as total,
                   p.method as paymentMethod, p.status as paymentStatus, o.status as orderStatus
            from CustomerOrder o left join Payment p on p.order = o
            where (:status is null or o.status = :status)
              and (:method is null or p.method = :method)
              and (:deliveryType is null or o.deliveryType = :deliveryType)
              and (:fromInstant is null or o.createdAt >= :fromInstant)
              and (:toInstant is null or o.createdAt < :toInstant)
              and (:query = '' or lower(o.publicNumber) like concat('%', :query, '%')
                   or lower(o.customerNameSnapshot) like concat('%', :query, '%'))
            order by o.createdAt desc
            """)
    List<OrderSummaryProjection> findOrderSummaries(@Param("status") OrderStatus status,
            @Param("method") PaymentMethod method, @Param("deliveryType") DeliveryType deliveryType,
            @Param("fromInstant") Instant fromInstant, @Param("toInstant") Instant toInstant,
            @Param("query") String query);

    @Query("""
            select c.id as id, c.name as name, c.phone as phone, c.pointsBalance as points,
                   count(o.id) as orderCount,
                   coalesce(sum(case when o.status = com.servifood.domain.model.OrderStatus.DELIVERED then o.total else 0 end), 0) as totalSpent,
                   max(o.createdAt) as lastOrderAt
            from Customer c left join CustomerOrder o on o.customer = c
            group by c.id, c.name, c.phone, c.pointsBalance
            order by c.name
            """)
    List<CustomerSummaryProjection> findCustomerSummaries();

    interface OrderSummaryProjection {
        String getPublicNumber(); String getCustomerName(); Instant getCreatedAt(); DeliveryType getDeliveryType();
        BigDecimal getTotal(); PaymentMethod getPaymentMethod(); PaymentStatus getPaymentStatus(); OrderStatus getOrderStatus();
    }
    interface CustomerSummaryProjection {
        Long getId(); String getName(); String getPhone(); int getPoints(); long getOrderCount();
        BigDecimal getTotalSpent(); Instant getLastOrderAt();
    }
}
