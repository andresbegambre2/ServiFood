package com.servifood.application;

import static com.servifood.presentation.rest.dto.AnalyticsDtos.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.DomainException;
import com.servifood.infrastructure.persistence.*;

@Service
public class AnalyticsService {
    private final AnalyticsQueryRepository analytics; private final BusinessSettingsRepository settings; private final IngredientRepository ingredients;
    public AnalyticsService(AnalyticsQueryRepository analytics, BusinessSettingsRepository settings, IngredientRepository ingredients) { this.analytics = analytics; this.settings = settings; this.ingredients = ingredients; }

    @Transactional(readOnly = true)
    public AnalyticsOverview overview(LocalDate from, LocalDate to) {
        ZoneId zone = zone(); LocalDate today = LocalDate.now(zone); Range selected = range(from, to, zone);
        PeriodMetric todayMetric = period("Hoy", today, today, today.minusDays(1), today.minusDays(1), zone);
        PeriodMetric yesterday = period("Ayer", today.minusDays(1), today.minusDays(1), today.minusDays(2), today.minusDays(2), zone);
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY); long weekDays = ChronoUnit.DAYS.between(weekStart, today) + 1;
        PeriodMetric week = period("Esta semana", weekStart, today, weekStart.minusDays(weekDays), weekStart.minusDays(1), zone);
        LocalDate monthStart = today.withDayOfMonth(1); long monthDays = ChronoUnit.DAYS.between(monthStart, today) + 1;
        PeriodMetric month = period("Este mes", monthStart, today, monthStart.minusDays(monthDays), monthStart.minusDays(1), zone);
        var aggregate = analytics.aggregate(selected.fromInstant(), selected.toExclusive());
        BigDecimal average = aggregate.deliveredOrders() == 0 ? money(BigDecimal.ZERO) : money(aggregate.sales().divide(BigDecimal.valueOf(aggregate.deliveredOrders()), 2, RoundingMode.HALF_UP));
        return new AnalyticsOverview(todayMetric, yesterday, week, month, aggregate.totalOrders(), aggregate.cancelledOrders(), average,
                aggregate.discounts(), analytics.couponUses(selected.fromInstant(), selected.toExclusive()), analytics.points(com.servifood.domain.model.LoyaltyMovementType.EARN, selected.fromInstant(), selected.toExclusive()),
                analytics.points(com.servifood.domain.model.LoyaltyMovementType.REDEEM, selected.fromInstant(), selected.toExclusive()), ingredients.countLowStock(), ingredients.countOutOfStock(),
                analytics.topProducts(selected.fromInstant(), selected.toExclusive(), 10), analytics.topCategories(selected.fromInstant(), selected.toExclusive(), 10), analytics.peakHours(selected.fromInstant(), selected.toExclusive()),
                translate(analytics.paymentMethods(selected.fromInstant(), selected.toExclusive())), translate(analytics.deliveryTypes(selected.fromInstant(), selected.toExclusive())),
                analytics.topCustomers(selected.fromInstant(), selected.toExclusive()), analytics.evolution(selected.fromInstant(), selected.toExclusive(), zone));
    }
    @Transactional(readOnly = true)
    public ReportData report(ReportType type, LocalDate from, LocalDate to) { Range range = range(from, to, zone()); return new ReportData(type, range.from(), range.to(), columns(type), analytics.report(type, range.fromInstant(), range.toExclusive())); }
    @Transactional(readOnly = true)
    public byte[] csv(ExportType type, LocalDate from, LocalDate to) {
        ReportData report = report(ReportType.valueOf(type.name()), from, to); StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(String.join(";", report.columns())).append("\r\n");
        report.rows().forEach(row -> { for (int index=0; index<row.size(); index++) { if (index>0) csv.append(';'); csv.append(escape(row.get(index))); } csv.append("\r\n"); });
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private PeriodMetric period(String label, LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo, ZoneId zone) {
        var current = analytics.aggregate(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
        var previous = analytics.aggregate(previousFrom.atStartOfDay(zone).toInstant(), previousTo.plusDays(1).atStartOfDay(zone).toInstant());
        BigDecimal change = previous.sales().signum() == 0 ? (current.sales().signum() == 0 ? BigDecimal.ZERO : new BigDecimal("100")) : current.sales().subtract(previous.sales()).multiply(new BigDecimal("100")).divide(previous.sales(), 2, RoundingMode.HALF_UP);
        return new PeriodMetric(label, from, to, current.sales(), previous.sales(), change, current.totalOrders());
    }
    private Range range(LocalDate from, LocalDate to, ZoneId zone) { LocalDate today = LocalDate.now(zone); LocalDate start = from == null ? today.minusDays(29) : from; LocalDate end = to == null ? today : to; if (start.isAfter(end)) throw new DomainException("La fecha inicial debe ser anterior a la final"); if (ChronoUnit.DAYS.between(start, end) > 366) throw new DomainException("El rango máximo es de 366 días"); return new Range(start, end, start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant()); }
    private ZoneId zone() { return ZoneId.of(settings.findFirstByOrderByIdAsc().map(value -> value.getTimeZone()).orElse("America/Bogota")); }
    private List<String> columns(ReportType type) { return switch (type) {
        case SALES -> List.of("Fecha", "Pedidos entregados", "Ventas", "Ticket promedio", "Descuentos");
        case ORDERS -> List.of("Pedido", "Fecha", "Estado", "Cliente", "Total", "Descuento");
        case PRODUCTS -> List.of("Producto", "Unidades", "Ventas");
        case CUSTOMERS -> List.of("Cliente", "Teléfono", "Pedidos", "Gasto total");
        case PROMOTIONS -> List.of("Promoción", "Tipo", "Valor", "Inicio", "Fin", "Activa");
        case COUPONS -> List.of("Cupón", "Usos", "Descuento total", "Clientes");
        case PAYMENTS -> List.of("Método de pago", "Pedidos", "Valor");
    }; }
    private List<RankedMetric> translate(List<RankedMetric> values) { return values.stream().map(value -> new RankedMetric(switch (value.label()) { case "CASH" -> "Efectivo"; case "TRANSFER" -> "Transferencia"; case "PAY_ON_PICKUP" -> "Pago al recoger"; case "DELIVERY" -> "Domicilio"; case "PICKUP" -> "Recoger"; default -> value.label(); }, value.quantity(), value.amount())).toList(); }
    private String escape(Object value) { String text = value == null ? "" : value instanceof Timestamp timestamp ? timestamp.toInstant().toString() : String.valueOf(value); return '"' + text.replace("\"", "\"\"") + '"'; }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private record Range(LocalDate from, LocalDate to, Instant fromInstant, Instant toExclusive) {}
}
