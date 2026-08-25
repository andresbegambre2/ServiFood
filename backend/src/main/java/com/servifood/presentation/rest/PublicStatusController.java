package com.servifood.presentation.rest;

import com.servifood.application.StatusService;
import com.servifood.presentation.rest.dto.ApiStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicStatusController {

    private final StatusService statusService;

    public PublicStatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiStatusResponse> status() {
        return ResponseEntity.ok(statusService.currentStatus());
    }
}

