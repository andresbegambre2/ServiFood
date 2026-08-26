package com.servifood.presentation.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.model.BusinessHours;
import com.servifood.domain.model.BusinessSettings;
import com.servifood.infrastructure.persistence.BusinessHoursRepository;
import com.servifood.infrastructure.persistence.BusinessSettingsRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.orders.allow-when-closed=false")
@AutoConfigureMockMvc
@Transactional
class PublicOrderClosedApiTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired BusinessSettingsRepository settings; @Autowired BusinessHoursRepository hours;

    @Test
    void rejectsOrdersOutsideConfiguredBusinessHours() throws Exception {
        BusinessSettings business = new BusinessSettings("Closed Test", "Demo", "+573005551212", "+573005551212", "Calle 1", new BigDecimal("5000.00"), 25, "COP");
        business.configureCheckout("America/Bogota", "Nequi", "Closed Test", "3005551212", null); settings.save(business);
        for (DayOfWeek day : DayOfWeek.values()) hours.save(new BusinessHours(day, 1, null, null, true));
        Map<String, Object> request = Map.of("clientRequestId", UUID.randomUUID().toString(), "customer", Map.of("name", "Ana", "phone", "3005551212"),
                "delivery", Map.of("type", "DELIVERY", "address", "Calle 1", "neighborhood", "Centro"),
                "payment", Map.of("method", "CASH"), "lines", List.of(Map.of("productId", 999, "quantity", 1, "notes", "", "extras", List.of())));
        MockMultipartFile order = new MockMultipartFile("order", "order.json", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(request));
        mvc.perform(multipart("/api/v1/public/orders").file(order)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESTAURANT_CLOSED"));
    }
}
