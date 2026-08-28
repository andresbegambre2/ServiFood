package com.servifood.infrastructure.persistence;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import static com.servifood.presentation.rest.dto.AnalyticsDtos.*;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.servifood.domain.model.*;

@Repository
public class AnalyticsQueryRepository {
    private final JdbcTemplate jdbc;
    public AnalyticsQueryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Aggregate aggregate(Instant from, Instant to) {
        return jdbc.queryForObject("""
                SELECT COUNT(*),
                    COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN total ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN discount ELSE 0 END), 0),
                    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END),
                    SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END)
                FROM orders WHERE created_at >= ? AND created_at < ?
                """, (rs, row) -> new Aggregate(rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getLong(4), rs.getLong(5)), timestamp(from), timestamp(to));
    }
    public List<RankedMetric> topProducts(Instant from, Instant to, int limit) { return ranked("""
            SELECT oi.product_name_snapshot, SUM(oi.quantity), SUM(oi.subtotal)
            FROM order_items oi JOIN orders o ON o.id = oi.order_id
            WHERE o.status = 'DELIVERED' AND o.created_at >= ? AND o.created_at < ?
            GROUP BY oi.product_name_snapshot ORDER BY SUM(oi.quantity) DESC
            """, from, to, limit); }
    public List<RankedMetric> topCategories(Instant from, Instant to, int limit) { return ranked("""
            SELECT c.name, SUM(oi.quantity), SUM(oi.subtotal)
            FROM order_items oi JOIN orders o ON o.id = oi.order_id JOIN products p ON p.id = oi.product_id JOIN categories c ON c.id = p.category_id
            WHERE o.status = 'DELIVERED' AND o.created_at >= ? AND o.created_at < ?
            GROUP BY c.id, c.name ORDER BY SUM(oi.quantity) DESC
            """, from, to, limit); }
    public List<RankedMetric> peakHours(Instant from, Instant to) { return ranked("""
            SELECT EXTRACT(HOUR FROM created_at), COUNT(*), COALESCE(SUM(total), 0)
            FROM orders WHERE status <> 'CANCELLED' AND created_at >= ? AND created_at < ?
            GROUP BY EXTRACT(HOUR FROM created_at) ORDER BY COUNT(*) DESC
            """, from, to, 8).stream().map(value -> new RankedMetric(String.format("%02d:00", Integer.parseInt(value.label())), value.quantity(), value.amount())).toList(); }
    public List<RankedMetric> paymentMethods(Instant from, Instant to) { return ranked("""
            SELECT p.method, COUNT(*), COALESCE(SUM(p.amount), 0)
            FROM payments p JOIN orders o ON o.id = p.order_id
            WHERE o.status <> 'CANCELLED' AND o.created_at >= ? AND o.created_at < ?
            GROUP BY p.method ORDER BY COUNT(*) DESC
            """, from, to, 10); }
    public List<RankedMetric> deliveryTypes(Instant from, Instant to) { return ranked("""
            SELECT delivery_type, COUNT(*), COALESCE(SUM(total), 0)
            FROM orders WHERE status <> 'CANCELLED' AND created_at >= ? AND created_at < ?
            GROUP BY delivery_type ORDER BY COUNT(*) DESC
            """, from, to, 10); }
    public List<CustomerMetric> topCustomers(Instant from, Instant to) { return jdbc.query("""
            SELECT c.id, c.name, COUNT(o.id), COALESCE(SUM(o.total), 0)
            FROM customers c JOIN orders o ON o.customer_id = c.id
            WHERE o.status = 'DELIVERED' AND o.created_at >= ? AND o.created_at < ?
            GROUP BY c.id, c.name ORDER BY SUM(o.total) DESC LIMIT 10
            """, (rs, row) -> new CustomerMetric(rs.getLong(1), rs.getString(2), rs.getLong(3), money(rs.getBigDecimal(4))), timestamp(from), timestamp(to)); }
    public List<TimePoint> evolution(Instant from, Instant to, ZoneId zone) { return jdbc.query("""
            SELECT CAST(created_at AS DATE), COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN total ELSE 0 END), 0), COUNT(*)
            FROM orders WHERE created_at >= ? AND created_at < ? GROUP BY CAST(created_at AS DATE) ORDER BY CAST(created_at AS DATE)
            """, (rs, row) -> new TimePoint(rs.getDate(1).toLocalDate(), money(rs.getBigDecimal(2)), rs.getLong(3)), timestamp(from), timestamp(to)); }
    public long couponUses(Instant from, Instant to) { return scalarLong("SELECT COUNT(*) FROM coupon_redemptions WHERE reversed_at IS NULL AND created_at >= ? AND created_at < ?", from, to); }
    public long points(LoyaltyMovementType type, Instant from, Instant to) { return scalarLong("SELECT COALESCE(SUM(ABS(points_delta)), 0) FROM loyalty_point_movements WHERE movement_type = ? AND created_at >= ? AND created_at < ?", type.name(), from, to); }
    public Operational operational(Instant from, Instant to) {
        Aggregate values = aggregate(from, to); long newOrders = scalarLong("SELECT COUNT(*) FROM orders WHERE status = 'NEW' AND created_at >= ? AND created_at < ?", from, to);
        long preparing = scalarLong("SELECT COUNT(*) FROM orders WHERE status = 'PREPARING' AND created_at >= ? AND created_at < ?", from, to);
        long review = scalarLong("SELECT COUNT(*) FROM payments WHERE status = 'UNDER_REVIEW' AND created_at >= ? AND created_at < ?", from, to);
        List<OrderSummary> latest = jdbc.query("""
                SELECT o.public_number,o.customer_name_snapshot,o.created_at,o.delivery_type,o.total,p.method,p.status,o.status
                FROM orders o LEFT JOIN payments p ON p.order_id=o.id WHERE o.created_at >= ? AND o.created_at < ? ORDER BY o.created_at DESC LIMIT 8
                """, (rs, row) -> new OrderSummary(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant(), DeliveryType.valueOf(rs.getString(4)), rs.getBigDecimal(5), rs.getString(6) == null ? null : PaymentMethod.valueOf(rs.getString(6)), rs.getString(7) == null ? null : PaymentStatus.valueOf(rs.getString(7)), OrderStatus.valueOf(rs.getString(8))), timestamp(from), timestamp(to));
        return new Operational(values, newOrders, preparing, review, latest, topProducts(from, to, 5));
    }
    public List<List<Object>> report(ReportType type, Instant from, Instant to) { return switch (type) {
        case SALES -> rows("SELECT CAST(created_at AS DATE), COUNT(*), COALESCE(SUM(total),0), COALESCE(AVG(total),0), COALESCE(SUM(discount),0) FROM orders WHERE status='DELIVERED' AND created_at>=? AND created_at<? GROUP BY CAST(created_at AS DATE) ORDER BY CAST(created_at AS DATE)", from, to, 5);
        case ORDERS -> rows("SELECT public_number,created_at,status,customer_name_snapshot,total,discount FROM orders WHERE created_at>=? AND created_at<? ORDER BY created_at DESC", from, to, 6);
        case PRODUCTS -> rows("SELECT oi.product_name_snapshot, SUM(oi.quantity), SUM(oi.subtotal) FROM order_items oi JOIN orders o ON o.id=oi.order_id WHERE o.status='DELIVERED' AND o.created_at>=? AND o.created_at<? GROUP BY oi.product_name_snapshot ORDER BY SUM(oi.quantity) DESC", from, to, 3);
        case CUSTOMERS -> rows("SELECT c.name,c.phone,COUNT(o.id),COALESCE(SUM(o.total),0) FROM customers c JOIN orders o ON o.customer_id=c.id WHERE o.status='DELIVERED' AND o.created_at>=? AND o.created_at<? GROUP BY c.id,c.name,c.phone ORDER BY SUM(o.total) DESC", from, to, 4);
        case PROMOTIONS -> rows("SELECT name,discount_type,discount_value,starts_at,ends_at,active FROM promotions WHERE starts_at<? AND ends_at>=? ORDER BY starts_at", to, from, 6);
        case COUPONS -> rows("SELECT c.code,COUNT(r.id),COALESCE(SUM(r.discount_amount),0),COUNT(DISTINCT r.customer_id) FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id WHERE r.reversed_at IS NULL AND r.created_at>=? AND r.created_at<? GROUP BY c.id,c.code ORDER BY COUNT(r.id) DESC", from, to, 4);
        case PAYMENTS -> rows("SELECT p.method,COUNT(*),COALESCE(SUM(p.amount),0) FROM payments p JOIN orders o ON o.id=p.order_id WHERE o.status<>'CANCELLED' AND o.created_at>=? AND o.created_at<? GROUP BY p.method ORDER BY COUNT(*) DESC", from, to, 3);
    }; }
    private List<RankedMetric> ranked(String sql, Instant from, Instant to, int limit) { List<RankedMetric> values = jdbc.query(sql, (rs, row) -> new RankedMetric(rs.getString(1), rs.getLong(2), money(rs.getBigDecimal(3))), timestamp(from), timestamp(to)); return values.stream().limit(limit).toList(); }
    private List<List<Object>> rows(String sql, Instant first, Instant second, int columns) { return jdbc.query(sql, (rs, row) -> { List<Object> values = new ArrayList<>(); for (int index=1; index<=columns; index++) values.add(rs.getObject(index)); return values; }, timestamp(first), timestamp(second)); }
    private long scalarLong(String sql, Instant from, Instant to) { Long value = jdbc.queryForObject(sql, Long.class, timestamp(from), timestamp(to)); return value == null ? 0 : value; }
    private long scalarLong(String sql, String type, Instant from, Instant to) { Long value = jdbc.queryForObject(sql, Long.class, type, timestamp(from), timestamp(to)); return value == null ? 0 : value; }
    private Timestamp timestamp(Instant value) { return Timestamp.from(value); } private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP); }
    public record Aggregate(long totalOrders, BigDecimal sales, BigDecimal discounts, long cancelledOrders, long deliveredOrders) {}
    public record Operational(Aggregate aggregate, long newOrders, long preparingOrders, long paymentsUnderReview, List<OrderSummary> latestOrders, List<RankedMetric> topProducts) {}
}
