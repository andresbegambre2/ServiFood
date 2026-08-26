package com.servifood.presentation.rest.error;

import java.net.URI;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.servifood.domain.exception.ResourceNotFoundException;
import com.servifood.domain.exception.DomainException;
import com.servifood.application.CheckoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException exception) { return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ProblemDetail> validation(Exception exception) { return problem(HttpStatus.BAD_REQUEST, "Validation failed", "The request contains invalid values."); }
    @ExceptionHandler(CheckoutException.class)
    ResponseEntity<ProblemDetail> checkout(CheckoutException exception) {
        ProblemDetail problem = detail(exception.getStatus(), "Checkout could not be completed", exception.getMessage());
        problem.setProperty("code", exception.getCode());
        if (exception.getCurrentQuote() != null) problem.setProperty("currentQuote", exception.getCurrentQuote());
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException exception) { return problem(HttpStatus.BAD_REQUEST, "Business rule failed", exception.getMessage()); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> uploadTooLarge(MaxUploadSizeExceededException exception) {
        ProblemDetail problem = detail(HttpStatus.PAYLOAD_TOO_LARGE, "Receipt is too large", "El comprobante supera el tamaño máximo permitido.");
        problem.setProperty("code", "INVALID_RECEIPT"); return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException exception) { return problem(HttpStatus.CONFLICT, "Conflict", "El registro entra en conflicto con datos existentes."); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = detail(status, title, detail);
        return ResponseEntity.status(status).body(problem);
    }
    private ProblemDetail detail(HttpStatus status, String title, String detail) { ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail); problem.setTitle(title); problem.setType(URI.create("https://servifood.dev/problems/" + status.value())); return problem; }
}
