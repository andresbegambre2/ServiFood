package com.servifood.application;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
public class AdminCatalogService {
    private final ProductRepository products; private final CategoryRepository categories; private final ExtraRepository extras;
    private final PromotionRepository promotions; private final ImageStorage images;
    public AdminCatalogService(ProductRepository products, CategoryRepository categories, ExtraRepository extras,
            PromotionRepository promotions, ImageStorage images) {
        this.products = products; this.categories = categories; this.extras = extras; this.promotions = promotions; this.images = images;
    }

    @Transactional(readOnly = true) public List<ProductView> products() { return products.findAllByOrderByNameAsc().stream().map(this::productView).toList(); }
    @Transactional(readOnly = true) public List<ExtraView> extras() { return extras.findAll().stream().map(extra -> new ExtraView(extra.getId(), extra.getName(), extra.getPrice(), extra.isAvailable())).toList(); }
    @Transactional public ProductView createProduct(ProductRequest request) {
        Category category = category(request.categoryId()); Set<Extra> allowed = allowedExtras(request.extraIds());
        Product product = new Product(request.name(), request.slug(), request.description(), request.price(), category);
        product.update(request.name(), request.slug(), request.description(), request.price(), category, request.available(), request.featured(), allowed);
        return productView(products.saveAndFlush(product));
    }
    @Transactional public ProductView updateProduct(Long id, ProductRequest request) {
        Product product = product(id); product.update(request.name(), request.slug(), request.description(), request.price(), category(request.categoryId()), request.available(), request.featured(), allowedExtras(request.extraIds()));
        return productView(products.saveAndFlush(product));
    }
    @Transactional public ProductView setProductActive(Long id, boolean active) { Product product = product(id); product.setAvailable(active); return productView(products.save(product)); }
    @Transactional public ProductView uploadImage(Long id, MultipartFile file) {
        Product product = product(id); String name = images.store(file); product.setImagePath("/api/v1/public/product-images/" + name); return productView(products.save(product));
    }

    @Transactional(readOnly = true) public List<CategoryView> categories() { return categories.findAllByOrderByDisplayOrderAsc().stream().map(this::categoryView).toList(); }
    @Transactional public CategoryView createCategory(CategoryRequest request) {
        Category category = new Category(request.name(), request.slug(), request.displayOrder()); category.update(request.name(), request.slug(), request.description(), request.displayOrder(), request.active());
        return categoryView(categories.saveAndFlush(category));
    }
    @Transactional public CategoryView updateCategory(Long id, CategoryRequest request) {
        Category category = category(id); category.update(request.name(), request.slug(), request.description(), request.displayOrder(), request.active()); return categoryView(categories.saveAndFlush(category));
    }
    @Transactional public CategoryView setCategoryActive(Long id, boolean active) {
        Category category = category(id); category.update(category.getName(), category.getSlug(), category.getDescription(), category.getDisplayOrder(), active); return categoryView(categories.save(category));
    }

    @Transactional(readOnly = true) public List<PromotionView> promotions() { return promotions.findAllByOrderByStartsAtDesc().stream().map(this::promotionView).toList(); }
    @Transactional public PromotionView createPromotion(PromotionRequest request) {
        Promotion promotion = new Promotion(request.name(), request.discountType(), request.discountValue(), request.startsAt(), request.endsAt(), request.minimumPurchase());
        promotion.update(request.name(), request.description(), request.discountType(), request.discountValue(), request.startsAt(), request.endsAt(), request.minimumPurchase(), request.usageLimit(), request.active());
        return promotionView(promotions.saveAndFlush(promotion));
    }
    @Transactional public PromotionView updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = promotion(id); promotion.update(request.name(), request.description(), request.discountType(), request.discountValue(), request.startsAt(), request.endsAt(), request.minimumPurchase(), request.usageLimit(), request.active());
        return promotionView(promotions.saveAndFlush(promotion));
    }
    @Transactional public PromotionView setPromotionActive(Long id, boolean active) {
        Promotion promotion = promotion(id); promotion.update(promotion.getName(), promotion.getDescription(), promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getStartsAt(), promotion.getEndsAt(), promotion.getMinimumPurchase(), promotion.getUsageLimit(), active);
        return promotionView(promotions.save(promotion));
    }

    private Product product(Long id) { return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id)); }
    private Category category(Long id) { return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id)); }
    private Promotion promotion(Long id) { return promotions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promotion", id)); }
    private Set<Extra> allowedExtras(Set<Long> ids) { if (ids == null || ids.isEmpty()) return Set.of(); List<Extra> found = extras.findAllById(ids); if (found.size() != ids.size()) throw new ResourceNotFoundException("Extra", ids); return new LinkedHashSet<>(found); }
    private ProductView productView(Product product) { return new ProductView(product.getId(), product.getName(), product.getSlug(), product.getDescription(), product.getPrice(), product.getImagePath(), product.isAvailable(), product.isFeatured(), product.getCategory().getId(), product.getCategory().getName(), product.getAllowedExtras().stream().map(Extra::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))); }
    private CategoryView categoryView(Category category) { return new CategoryView(category.getId(), category.getName(), category.getSlug(), category.getDescription(), category.getDisplayOrder(), category.isActive()); }
    private PromotionView promotionView(Promotion promotion) { return new PromotionView(promotion.getId(), promotion.getName(), promotion.getDescription(), promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getStartsAt(), promotion.getEndsAt(), promotion.getMinimumPurchase(), promotion.getUsageLimit(), promotion.isActive()); }
}
