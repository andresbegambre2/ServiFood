package com.servifood.presentation.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}
    public enum ReportType { SALES, ORDERS, PRODUCTS, CUSTOMERS, PROMOTIONS, COUPONS, PAYMENTS }
    public enum ExportType { SALES, ORDERS, PRODUCTS, CUSTOMERS, COUPONS }
    public record PeriodMetric(String label, LocalDate from, LocalDate to, BigDecimal sales, BigDecimal previousSales, BigDecimal changePercentage, long orders) {}
    public record RankedMetric(String label, long quantity, BigDecimal amount) {}
    public record CustomerMetric(Long customerId, String name, long orders, BigDecimal amount) {}
    public record TimePoint(LocalDate date, BigDecimal sales, long orders) {}
    public record AnalyticsOverview(PeriodMetric today, PeriodMetric yesterday, PeriodMetric week, PeriodMetric month,
            long totalOrders, long cancelledOrders, BigDecimal averageTicket, BigDecimal discountsApplied,
            long couponUses, long pointsEarned, long pointsRedeemed, long lowStockIngredients, long outOfStockIngredients,
            List<RankedMetric> topProducts, List<RankedMetric> topCategories, List<RankedMetric> peakHours,
            List<RankedMetric> paymentMethods, List<RankedMetric> deliveryTypes, List<CustomerMetric> topCustomers,
            List<TimePoint> salesEvolution) {}
    public record ReportData(ReportType type, LocalDate from, LocalDate to, List<String> columns, List<List<Object>> rows) {}
}
