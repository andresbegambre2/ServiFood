package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.KitchenDtos.*;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.KitchenOrderService;

@RestController
@RequestMapping("/api/v1/admin/kitchen")
public class KitchenController {
    private final KitchenOrderService service;
    public KitchenController(KitchenOrderService service) { this.service = service; }

    @GetMapping("/orders")
    List<KitchenOrder> orders() { return service.list(); }

    @PatchMapping("/orders/{number}/status")
    KitchenOrder status(@PathVariable String number, @Valid @RequestBody KitchenStatusChange request) {
        return service.changeStatus(number, request.status());
    }
}
