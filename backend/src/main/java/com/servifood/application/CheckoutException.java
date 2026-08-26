package com.servifood.application;

import org.springframework.http.HttpStatus;
import com.servifood.presentation.rest.dto.CheckoutQuoteResponse;

public class CheckoutException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final CheckoutQuoteResponse currentQuote;

    public CheckoutException(HttpStatus status, String code, String message) { this(status, code, message, null); }
    public CheckoutException(HttpStatus status, String code, String message, CheckoutQuoteResponse currentQuote) {
        super(message); this.status = status; this.code = code; this.currentQuote = currentQuote;
    }
    public String getCode() { return code; } public HttpStatus getStatus() { return status; }
    public CheckoutQuoteResponse getCurrentQuote() { return currentQuote; }
}
