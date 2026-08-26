package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.AdminOrderService;
import com.servifood.domain.model.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOrderController {
    private final AdminOrderService service;
    public AdminOrderController(AdminOrderService service) { this.service = service; }

    @GetMapping("/dashboard") Dashboard dashboard() { return service.dashboard(); }
    @GetMapping("/orders") List<OrderSummary> orders(@RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod, @RequestParam(required = false) DeliveryType deliveryType,
            @RequestParam(required = false) LocalDate date, @RequestParam(required = false) String query) {
        return service.list(status, paymentMethod, deliveryType, date, query);
    }
    @GetMapping("/orders/{number}") OrderDetail order(@PathVariable String number) { return service.detail(number); }
    @PatchMapping("/orders/{number}/status") OrderDetail status(@PathVariable String number, @Valid @RequestBody StatusChange request) { return service.changeStatus(number, request.status(), request.reason()); }
    @GetMapping("/payments") List<PaymentQueueItem> payments(@RequestParam(required = false) PaymentStatus status) { return service.paymentQueue(status); }
    @PostMapping("/orders/{number}/payment/approve") OrderDetail approve(@PathVariable String number, Authentication authentication) { return service.approvePayment(number, authentication); }
    @PostMapping("/orders/{number}/payment/reject") OrderDetail reject(@PathVariable String number, @Valid @RequestBody RejectPayment request, Authentication authentication) { return service.rejectPayment(number, request.reason(), authentication); }
    @GetMapping("/orders/{number}/payment/receipt") ResponseEntity<byte[]> receipt(@PathVariable String number) {
        var file = service.receipt(number);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt").body(file.content());
    }
}
