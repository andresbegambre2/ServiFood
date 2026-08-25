package com.servifood.application;

import com.servifood.presentation.rest.dto.ApiStatusResponse;
import org.springframework.stereotype.Service;

@Service
public class StatusService {

    public ApiStatusResponse currentStatus() {
        return new ApiStatusResponse("ServiFood", "available");
    }
}

