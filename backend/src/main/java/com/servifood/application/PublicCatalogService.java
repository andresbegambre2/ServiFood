package com.servifood.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;
import com.servifood.presentation.rest.dto.*;

@Service
@Transactional(readOnly = true)
public class PublicCatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final PromotionRepository promotions;
    private final BusinessSettingsRepository settings;
    private final BusinessHoursRepository hours;

    public PublicCatalogService(CategoryRepository categories, ProductRepository products, PromotionRepository promotions,
            BusinessSettingsRepository settings, BusinessHoursRepository hours) {
        this.categories = categories; this.products = products; this.promotions = promotions; this.settings = settings; this.hours = hours;
    }

    public List<CategoryResponse> activeCategories() { return categories.findByActiveTrueOrderByDisplayOrderAsc().stream().map(this::category).toList(); }
    public List<ProductSummaryResponse> availableProducts() { return products.findByAvailableTrueOrderByFeaturedDescNameAsc().stream().map(this::summary).toList(); }
    public List<ProductSummaryResponse> featuredProducts() { return products.findByAvailableTrueAndFeaturedTrueOrderByNameAsc().stream().map(this::summary).toList(); }
    public ProductDetailResponse productBySlug(String slug) { return detail(products.findBySlugAndAvailableTrue(slug).orElseThrow(() -> new ResourceNotFoundException("Product", slug))); }
    public List<PromotionResponse> activePromotions() { Instant now = Instant.now(); return promotions.findByActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByEndsAtAsc(now, now).stream().map(this::promotion).toList(); }
    public BusinessPublicResponse publicBusiness() {
        BusinessSettings business = settings.findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("Business settings", "default"));
        List<BusinessHoursResponse> schedule = hours.findAll().stream().sorted(Comparator.comparing(BusinessHours::getDayOfWeek).thenComparingInt(BusinessHours::getSlotNumber)).map(this::schedule).toList();
        boolean transferConfigured = business.getTransferProvider() != null && business.getTransferAccountHolder() != null
                && business.getTransferAccountReference() != null;
        TransferPaymentResponse transfer = new TransferPaymentResponse(business.getTransferProvider(),
                business.getTransferAccountHolder(), business.getTransferAccountReference(), business.getPaymentQrPath(), transferConfigured);
        return new BusinessPublicResponse(business.getTradeName(), business.getDescription(), business.getLogoPath(), business.getPhone(), business.getWhatsapp(), business.getAddress(), business.getInstagram(), business.getFacebook(), business.getBaseDeliveryFee(), business.getEstimatedPreparationMinutes(), business.getCurrency(), business.getTimeZone(), transfer, schedule);
    }

    private CategoryResponse category(Category value) { return new CategoryResponse(value.getId(), value.getName(), value.getSlug(), value.getDescription()); }
    private ProductSummaryResponse summary(Product value) { return new ProductSummaryResponse(value.getId(), value.getName(), value.getSlug(), value.getDescription(), value.getPrice(), value.getImagePath(), value.isAvailable(), value.isFeatured(), category(value.getCategory())); }
    private ProductDetailResponse detail(Product value) { return new ProductDetailResponse(value.getId(), value.getName(), value.getSlug(), value.getDescription(), value.getPrice(), value.getImagePath(), value.isAvailable(), value.isFeatured(), category(value.getCategory()), value.getAllowedExtras().stream().filter(Extra::isAvailable).sorted(Comparator.comparing(Extra::getName)).map(extra -> new ExtraResponse(extra.getId(), extra.getName(), extra.getDescription(), extra.getPrice())).toList()); }
    private PromotionResponse promotion(Promotion value) { return new PromotionResponse(value.getId(), value.getName(), value.getDescription(), value.getDiscountType(), value.getDiscountValue(), value.getStartsAt(), value.getEndsAt(), value.getMinimumPurchase()); }
    private BusinessHoursResponse schedule(BusinessHours value) { return new BusinessHoursResponse(value.getDayOfWeek(), value.getSlotNumber(), value.getOpensAt(), value.getClosesAt(), value.isClosed()); }
}
