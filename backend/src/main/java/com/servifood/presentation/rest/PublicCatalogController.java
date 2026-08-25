package com.servifood.presentation.rest;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.PublicCatalogService;
import com.servifood.presentation.rest.dto.*;

@RestController
@RequestMapping("/api/v1/public")
public class PublicCatalogController {
    private final PublicCatalogService catalog;
    public PublicCatalogController(PublicCatalogService catalog) { this.catalog = catalog; }
    @GetMapping("/business") public ResponseEntity<BusinessPublicResponse> business() { return ResponseEntity.ok(catalog.publicBusiness()); }
    @GetMapping("/categories") public ResponseEntity<List<CategoryResponse>> categories() { return ResponseEntity.ok(catalog.activeCategories()); }
    @GetMapping("/products") public ResponseEntity<List<ProductSummaryResponse>> products() { return ResponseEntity.ok(catalog.availableProducts()); }
    @GetMapping("/products/featured") public ResponseEntity<List<ProductSummaryResponse>> featured() { return ResponseEntity.ok(catalog.featuredProducts()); }
    @GetMapping("/products/{slug}") public ResponseEntity<ProductDetailResponse> product(@PathVariable String slug) { return ResponseEntity.ok(catalog.productBySlug(slug)); }
    @GetMapping("/promotions") public ResponseEntity<List<PromotionResponse>> promotions() { return ResponseEntity.ok(catalog.activePromotions()); }
}
