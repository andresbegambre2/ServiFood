package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.KitchenDtos.*;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.KitchenOrderService;

@RestController
@RequestMapping("/api/v1/kitchen/orders")
public class KitchenOrderController {
    private final KitchenOrderService service;

    public KitchenOrderController(KitchenOrderService service) { this.service = service; }

    @GetMapping
    List<KitchenOrder> activeOrders() { return service.activeOrders(); }

    @PatchMapping("/{publicNumber}/stage")
    KitchenOrder transition(@PathVariable String publicNumber, @Valid @RequestBody KitchenStageChange request) {
        return service.transition(publicNumber, request.target());
    }
}
