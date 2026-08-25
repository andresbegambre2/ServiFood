package com.servifood.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;
import com.servifood.presentation.rest.dto.*;

@Service
public class PublicOrderService {
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private final CustomerOrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final PromotionRepository promotions;
    private final BusinessSettingsRepository settings;
    private final BusinessHoursRepository hours;
    private final PaymentRepository payments;
    private final ReceiptStorage receipts;
    private final Clock clock;
    private final boolean allowWhenClosed;
    private final byte[] trackingSecret;

    @Autowired
    public PublicOrderService(CustomerOrderRepository orders, CustomerRepository customers, ProductRepository products,
            PromotionRepository promotions, BusinessSettingsRepository settings, BusinessHoursRepository hours,
            PaymentRepository payments, ReceiptStorage receipts,
            @Value("${app.orders.allow-when-closed:false}") boolean allowWhenClosed,
            @Value("${app.orders.tracking-secret}") String trackingSecret) {
        this(orders, customers, products, promotions, settings, hours, payments, receipts, Clock.systemUTC(),
                allowWhenClosed, trackingSecret);
    }

    PublicOrderService(CustomerOrderRepository orders, CustomerRepository customers, ProductRepository products,
            PromotionRepository promotions, BusinessSettingsRepository settings, BusinessHoursRepository hours,
            PaymentRepository payments, ReceiptStorage receipts, Clock clock, boolean allowWhenClosed, String trackingSecret) {
        if (trackingSecret == null || trackingSecret.length() < 32) throw new IllegalArgumentException("tracking secret must contain at least 32 characters");
        this.orders = orders; this.customers = customers; this.products = products; this.promotions = promotions;
        this.settings = settings; this.hours = hours; this.payments = payments; this.receipts = receipts; this.clock = clock;
        this.allowWhenClosed = allowWhenClosed; this.trackingSecret = trackingSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public CheckoutQuoteResponse quote(CheckoutQuoteRequest request) {
        return calculate(request.deliveryType(), request.lines(), false).response();
    }

    @Transactional
    public synchronized OrderCreatedResponse create(CreateOrderRequest request, MultipartFile receipt) {
        String requestId = request.clientRequestId().toString();
        var existing = orders.findByClientRequestId(requestId);
        if (existing.isPresent()) return created(existing.get(), paymentFor(existing.get()), true);

        validateCheckout(request, receipt);
        BusinessSettings business = business();
        assertOpen(business);
        Calculation calculation = calculate(request.delivery().type(), request.lines(), true);
        String storedReceipt = null;
        try {
            if (request.payment().method() == PaymentMethod.TRANSFER) storedReceipt = receipts.store(receipt);
            Customer customer = customer(request.customer(), request.delivery());
            String publicNumber = nextPublicNumber(business.getTimeZone());
            String token = trackingToken(publicNumber, requestId);
            String address = deliverySnapshot(request.delivery());
            CustomerOrder order = new CustomerOrder(publicNumber, customer, request.customer().name().trim(),
                    normalizePhone(request.customer().phone()), request.delivery().type(), address,
                    calculation.response().totals().deliveryFee());
            order.configurePublicCheckout(requestId, sha256(token), blankToNull(request.customer().email()),
                    calculation.response().totals().estimatedMinutes());
            calculation.items().forEach(order::addItem);
            order.applyDiscount(calculation.response().totals().discount());
            orders.saveAndFlush(order);

            Payment payment = new Payment(order, request.payment().method(), order.getTotal());
            if (request.payment().method() == PaymentMethod.TRANSFER) payment.submitForReview(storedReceipt);
            if (request.payment().method() == PaymentMethod.CASH) payment.recordCashTendered(request.payment().cashTendered());
            payments.save(payment);
            return created(order, payment, false);
        } catch (RuntimeException exception) {
            if (storedReceipt != null) receipts.deleteQuietly(storedReceipt);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse tracking(String publicNumber, String token) {
        CustomerOrder order = orders.findByPublicNumber(publicNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", publicNumber));
        if (order.getClientRequestId() == null || order.getTrackingTokenHash() == null) {
            throw new ResourceNotFoundException("Order", publicNumber);
        }
        String expected = trackingToken(order.getPublicNumber(), order.getClientRequestId());
        if (token == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))
                || !MessageDigest.isEqual(order.getTrackingTokenHash().getBytes(StandardCharsets.UTF_8), sha256(token).getBytes(StandardCharsets.UTF_8))) {
            throw new ResourceNotFoundException("Order", publicNumber);
        }
        Payment payment = paymentFor(order);
        BusinessSettings business = business();
        return new OrderTrackingResponse(order.getPublicNumber(), order.getStatus(), payment.getMethod(), payment.getStatus(),
                order.getDeliveryType(), order.getDeliveryAddressSnapshot(), order.getCustomerNameSnapshot(), order.getCreatedAt(),
                totals(order, business), itemResponses(order.getItems()), business.getWhatsapp());
    }

    private Calculation calculate(DeliveryType deliveryType, List<OrderLineRequest> requests, boolean rejectPriceChanges) {
        BusinessSettings business = business();
        List<OrderItem> items = new ArrayList<>();
        boolean priceChanged = false;
        for (OrderLineRequest line : requests) {
            Product product = products.findById(line.productId()).orElseThrow(() -> unavailable("PRODUCT_UNAVAILABLE", "Un producto ya no está disponible."));
            if (!product.isAvailable()) throw unavailable("PRODUCT_UNAVAILABLE", product.getName() + " está agotado.");
            if (line.expectedUnitPrice() != null && money(line.expectedUnitPrice()).compareTo(money(product.getPrice())) != 0) priceChanged = true;
            OrderItem item = new OrderItem(product, line.quantity(), blankToNull(line.notes()));
            Set<Long> selected = new HashSet<>();
            for (OrderExtraRequest requestedExtra : line.extras()) {
                if (!selected.add(requestedExtra.extraId())) throw new CheckoutException(HttpStatus.BAD_REQUEST, "DUPLICATE_EXTRA", "Un extra no puede repetirse en la misma línea.");
                Extra extra = product.getAllowedExtras().stream().filter(value -> value.getId().equals(requestedExtra.extraId())).findFirst()
                        .orElseThrow(() -> unavailable("EXTRA_UNAVAILABLE", "Un extra ya no está permitido para " + product.getName() + "."));
                if (!extra.isAvailable()) throw unavailable("EXTRA_UNAVAILABLE", extra.getName() + " ya no está disponible.");
                if (requestedExtra.expectedUnitPrice() != null && money(requestedExtra.expectedUnitPrice()).compareTo(money(extra.getPrice())) != 0) priceChanged = true;
                item.addExtra(new OrderItemExtra(extra, line.quantity()));
            }
            items.add(item);
        }
        BigDecimal subtotal = items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = bestDiscount(subtotal);
        BigDecimal deliveryFee = deliveryType == DeliveryType.DELIVERY ? money(business.getBaseDeliveryFee()) : money(BigDecimal.ZERO);
        BigDecimal total = money(subtotal.add(deliveryFee).subtract(discount).max(BigDecimal.ZERO));
        int estimate = business.getEstimatedPreparationMinutes() + (deliveryType == DeliveryType.DELIVERY ? 15 : 0);
        CheckoutQuoteResponse response = new CheckoutQuoteResponse(
                new OrderTotalsResponse(money(subtotal), money(discount), deliveryFee, total, business.getCurrency(), estimate),
                itemResponses(items));
        if (rejectPriceChanges && priceChanged) throw new CheckoutException(HttpStatus.CONFLICT, "PRICE_CHANGED", "Algunos precios cambiaron. Revisa el total actualizado.", response);
        return new Calculation(items, response);
    }

    private BigDecimal bestDiscount(BigDecimal subtotal) {
        Instant now = clock.instant();
        return promotions.findByActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByEndsAtAsc(now, now).stream()
                .filter(promotion -> subtotal.compareTo(promotion.getMinimumPurchase()) >= 0)
                .map(promotion -> promotion.getDiscountType() == DiscountType.PERCENTAGE
                        ? subtotal.multiply(promotion.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                        : promotion.getDiscountValue())
                .map(value -> value.min(subtotal)).max(Comparator.naturalOrder()).map(this::money).orElse(money(BigDecimal.ZERO));
    }

    private void validateCheckout(CreateOrderRequest request, MultipartFile receipt) {
        DeliveryCheckoutRequest delivery = request.delivery(); PaymentCheckoutRequest payment = request.payment();
        if (delivery.type() == DeliveryType.DELIVERY && (isBlank(delivery.address()) || isBlank(delivery.neighborhood())))
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "INVALID_ADDRESS", "Dirección y barrio son obligatorios para domicilio.");
        if (delivery.type() == DeliveryType.DELIVERY && payment.method() == PaymentMethod.PAY_ON_PICKUP)
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD", "Pago al recoger solo aplica para pedidos en el local.");
        if (delivery.type() == DeliveryType.PICKUP && payment.method() == PaymentMethod.CASH)
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD", "El efectivo aplica únicamente para domicilios.");
        if (payment.method() == PaymentMethod.TRANSFER && !transferConfigured(business()))
            throw new CheckoutException(HttpStatus.CONFLICT, "TRANSFER_NOT_CONFIGURED", "La transferencia no está disponible en este momento.");
        if (payment.method() != PaymentMethod.TRANSFER && receipt != null && !receipt.isEmpty())
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "UNEXPECTED_RECEIPT", "El comprobante solo aplica a transferencias.");
    }

    private Customer customer(CustomerCheckoutRequest request, DeliveryCheckoutRequest delivery) {
        String phone = normalizePhone(request.phone());
        Customer customer = customers.findFirstByPhone(phone).orElseGet(() -> new Customer(request.name().trim(), phone, blankToNull(request.email())));
        if (delivery.type() == DeliveryType.DELIVERY) customer.addAddress(new CustomerAddress("Pedido web", delivery.address().trim(), delivery.neighborhood().trim(), blankToNull(delivery.reference()), false));
        return customers.save(customer);
    }

    private void assertOpen(BusinessSettings business) {
        if (allowWhenClosed) return;
        ZoneId zone;
        try { zone = ZoneId.of(business.getTimeZone()); } catch (Exception exception) { throw new CheckoutException(HttpStatus.SERVICE_UNAVAILABLE, "INVALID_BUSINESS_TIME_ZONE", "La zona horaria del restaurante no es válida."); }
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone)); LocalTime time = now.toLocalTime();
        boolean open = hours.findByDayOfWeekOrderBySlotNumber(now.getDayOfWeek()).stream()
                .anyMatch(slot -> !slot.isClosed() && !time.isBefore(slot.getOpensAt()) && time.isBefore(slot.getClosesAt()));
        if (!open) throw new CheckoutException(HttpStatus.CONFLICT, "RESTAURANT_CLOSED", "El restaurante está cerrado en este momento. Tu carrito sigue guardado.");
    }

    private String nextPublicNumber(String timeZone) {
        String prefix = "SF-" + ZonedDateTime.now(clock.withZone(ZoneId.of(timeZone))).format(NUMBER_DATE) + "-";
        for (int attempt = 0; attempt < 20; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            String candidate = prefix + suffix; if (!orders.existsByPublicNumber(candidate)) return candidate;
        }
        throw new CheckoutException(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_NUMBER_ERROR", "No pudimos generar el número del pedido.");
    }

    private OrderCreatedResponse created(CustomerOrder order, Payment payment, boolean idempotent) {
        BusinessSettings business = business(); String token = trackingToken(order.getPublicNumber(), order.getClientRequestId());
        return new OrderCreatedResponse(order.getPublicNumber(), token, order.getStatus(), payment.getMethod(), payment.getStatus(),
                order.getDeliveryType(), order.getDeliveryAddressSnapshot(), order.getCustomerNameSnapshot(), order.getCreatedAt(),
                totals(order, business), itemResponses(order.getItems()), business.getWhatsapp(), idempotent);
    }
    private Payment paymentFor(CustomerOrder order) { return payments.findFirstByOrderId(order.getId()).orElseThrow(() -> new ResourceNotFoundException("Payment", order.getPublicNumber())); }
    private OrderTotalsResponse totals(CustomerOrder order, BusinessSettings business) { return new OrderTotalsResponse(order.getSubtotal(), order.getDiscount(), order.getDeliveryFee(), order.getTotal(), business.getCurrency(), order.getEstimatedMinutes()); }
    private List<OrderItemSnapshotResponse> itemResponses(List<OrderItem> items) { return items.stream().map(item -> new OrderItemSnapshotResponse(item.getProductNameSnapshot(), item.getUnitPriceSnapshot(), item.getQuantity(), item.getNotes(), item.getSubtotal(), item.getExtras().stream().map(extra -> new OrderExtraSnapshotResponse(extra.getExtraNameSnapshot(), extra.getUnitPriceSnapshot(), extra.getQuantity(), extra.getSubtotal())).toList())).toList(); }
    private BusinessSettings business() { return settings.findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("Business settings", "default")); }
    private boolean transferConfigured(BusinessSettings value) { return !isBlank(value.getTransferProvider()) && !isBlank(value.getTransferAccountHolder()) && !isBlank(value.getTransferAccountReference()); }
    private String deliverySnapshot(DeliveryCheckoutRequest delivery) { if (delivery.type() == DeliveryType.PICKUP) return null; return delivery.address().trim() + " · " + delivery.neighborhood().trim() + (isBlank(delivery.reference()) ? "" : " · " + delivery.reference().trim()); }
    private String normalizePhone(String value) { return value.replaceAll("[^0-9+]", ""); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private CheckoutException unavailable(String code, String message) { return new CheckoutException(HttpStatus.CONFLICT, code, message); }
    private String trackingToken(String publicNumber, String requestId) {
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(trackingSecret, "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((publicNumber + ":" + requestId).getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException("HMAC is unavailable", exception); }
    }
    private String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
    private record Calculation(List<OrderItem> items, CheckoutQuoteResponse response) {}
}
