package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.LoyaltyDtos.*;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.AdminLoyaltyService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminLoyaltyController {
    private final AdminLoyaltyService service;
    public AdminLoyaltyController(AdminLoyaltyService service) { this.service = service; }
    @GetMapping("/customers") List<CustomerSummary> customers() { return service.customers(); }
    @GetMapping("/customers/{id}") CustomerProfile customer(@PathVariable Long id) { return service.customer(id); }
    @PostMapping("/customers/{id}/points") CustomerProfile adjust(@PathVariable Long id, @Valid @RequestBody PointAdjustment request, Authentication authentication) { return service.adjust(id, request, authentication); }
    @GetMapping("/customers/{id}/orders/{publicNumber}/repeat") RepeatOrderResponse repeat(@PathVariable Long id, @PathVariable String publicNumber) { return service.repeat(id, publicNumber); }
    @GetMapping("/coupons") List<CouponView> coupons() { return service.coupons(); }
    @PostMapping("/coupons") CouponView createCoupon(@Valid @RequestBody CouponRequest request) { return service.createCoupon(request); }
    @PutMapping("/coupons/{id}") CouponView updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) { return service.updateCoupon(id, request); }
    @GetMapping("/loyalty/settings") LoyaltySettingsView settings() { return service.settings(); }
    @PutMapping("/loyalty/settings") LoyaltySettingsView updateSettings(@Valid @RequestBody LoyaltySettingsRequest request) { return service.updateSettings(request); }
}
