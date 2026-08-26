package com.servifood.application;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.domain.exception.DomainException;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.model.*;
import com.servifood.infrastructure.persistence.*;

@Service
public class AdminSettingsService {
    private final BusinessSettingsRepository settings; private final BusinessHoursRepository hours; private final ImageStorage images;
    public AdminSettingsService(BusinessSettingsRepository settings, BusinessHoursRepository hours, ImageStorage images) { this.settings = settings; this.hours = hours; this.images = images; }

    @Transactional(readOnly = true) public SettingsView get() { return view(current()); }

    @Transactional
    public SettingsView update(SettingsRequest request) {
        BusinessSettings value = current();
        value.updatePublicDetails(request.tradeName(), request.description(), request.phone(), request.whatsapp(), request.address(), request.instagram(), request.facebook(), request.baseDeliveryFee(), request.estimatedPreparationMinutes());
        value.configureCheckout(request.timeZone(), request.transferProvider(), request.transferAccountHolder(), request.transferAccountReference(), value.getPaymentQrPath());
        settings.save(value);
        if (request.hours() != null) {
            hours.deleteAllInBatch();
            for (HoursRequest item : request.hours()) {
                DayOfWeek day;
                try { day = DayOfWeek.valueOf(item.dayOfWeek()); } catch (IllegalArgumentException exception) { throw new DomainException("Día de la semana inválido"); }
                hours.save(new BusinessHours(day, item.slotNumber(), item.opensAt(), item.closesAt(), item.closed()));
            }
        }
        return view(value);
    }

    @Transactional
    public SettingsView uploadQr(MultipartFile file) {
        BusinessSettings value = current(); String name = images.store(file);
        value.configureCheckout(value.getTimeZone(), value.getTransferProvider(), value.getTransferAccountHolder(), value.getTransferAccountReference(), "/api/v1/public/product-images/" + name);
        return view(settings.save(value));
    }

    private BusinessSettings current() { return settings.findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("Business settings", "primary")); }
    private SettingsView view(BusinessSettings value) {
        List<HoursView> schedule = hours.findAll().stream().sorted(Comparator.comparing(BusinessHours::getDayOfWeek).thenComparingInt(BusinessHours::getSlotNumber))
                .map(item -> new HoursView(item.getId(), item.getDayOfWeek().name(), item.getSlotNumber(), item.getOpensAt(), item.getClosesAt(), item.isClosed())).toList();
        return new SettingsView(value.getTradeName(), value.getDescription(), value.getPhone(), value.getWhatsapp(), value.getAddress(), value.getInstagram(), value.getFacebook(), value.getBaseDeliveryFee(), value.getEstimatedPreparationMinutes(), value.getTimeZone(), value.getTransferProvider(), value.getTransferAccountHolder(), value.getTransferAccountReference(), value.getPaymentQrPath(), schedule);
    }
}
