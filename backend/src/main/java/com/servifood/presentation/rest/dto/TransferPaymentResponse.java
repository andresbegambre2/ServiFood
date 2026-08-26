package com.servifood.presentation.rest.dto;

public record TransferPaymentResponse(String provider, String accountHolder, String accountReference,
        String qrPath, boolean configured) {}
