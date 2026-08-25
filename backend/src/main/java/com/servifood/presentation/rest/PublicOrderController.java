package com.servifood.presentation.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.application.PublicOrderService;
import com.servifood.presentation.rest.dto.*;

@RestController
@RequestMapping("/api/v1/public/orders")
public class PublicOrderController {
    private final PublicOrderService service;
    public PublicOrderController(PublicOrderService service) { this.service = service; }

    @PostMapping(path = "/quote", consumes = MediaType.APPLICATION_JSON_VALUE)
    CheckoutQuoteResponse quote(@Valid @RequestBody CheckoutQuoteRequest request) { return service.quote(request); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<OrderCreatedResponse> create(@Valid @RequestPart("order") CreateOrderRequest request,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt) {
        OrderCreatedResponse response = service.create(request, receipt);
        return ResponseEntity.status(response.idempotent() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{publicNumber}")
    OrderTrackingResponse tracking(@PathVariable String publicNumber, @RequestParam String token) {
        return service.tracking(publicNumber, token);
    }
}
