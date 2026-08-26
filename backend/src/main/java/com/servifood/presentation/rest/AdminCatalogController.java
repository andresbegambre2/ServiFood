package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.application.AdminCatalogService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final AdminCatalogService service;
    public AdminCatalogController(AdminCatalogService service) { this.service = service; }

    @GetMapping("/products") List<ProductView> products() { return service.products(); }
    @GetMapping("/extras") List<ExtraView> extras() { return service.extras(); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED) ProductView createProduct(@Valid @RequestBody ProductRequest request) { return service.createProduct(request); }
    @PutMapping("/products/{id}") ProductView updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return service.updateProduct(id, request); }
    @PatchMapping("/products/{id}/active") ProductView productActive(@PathVariable Long id, @RequestBody ActiveRequest request) { return service.setProductActive(id, request.active()); }
    @PostMapping(path = "/products/{id}/image", consumes = "multipart/form-data") ProductView productImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) { return service.uploadImage(id, image); }

    @GetMapping("/categories") List<CategoryView> categories() { return service.categories(); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) CategoryView createCategory(@Valid @RequestBody CategoryRequest request) { return service.createCategory(request); }
    @PutMapping("/categories/{id}") CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) { return service.updateCategory(id, request); }
    @PatchMapping("/categories/{id}/active") CategoryView categoryActive(@PathVariable Long id, @RequestBody ActiveRequest request) { return service.setCategoryActive(id, request.active()); }

    @GetMapping("/promotions") List<PromotionView> promotions() { return service.promotions(); }
    @PostMapping("/promotions") @ResponseStatus(HttpStatus.CREATED) PromotionView createPromotion(@Valid @RequestBody PromotionRequest request) { return service.createPromotion(request); }
    @PutMapping("/promotions/{id}") PromotionView updatePromotion(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) { return service.updatePromotion(id, request); }
    @PatchMapping("/promotions/{id}/active") PromotionView promotionActive(@PathVariable Long id, @RequestBody ActiveRequest request) { return service.setPromotionActive(id, request.active()); }
}
