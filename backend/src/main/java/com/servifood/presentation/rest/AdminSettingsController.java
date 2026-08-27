package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.AdminDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.servifood.application.AdminSettingsService;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminSettingsController {
    private final AdminSettingsService service;
    public AdminSettingsController(AdminSettingsService service) { this.service = service; }
    @GetMapping SettingsView get() { return service.get(); }
    @PutMapping SettingsView update(@Valid @RequestBody SettingsRequest request) { return service.update(request); }
    @PostMapping(path = "/qr", consumes = "multipart/form-data") SettingsView qr(@RequestPart("image") MultipartFile image) { return service.uploadQr(image); }
}
