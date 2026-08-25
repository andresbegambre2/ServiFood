package com.servifood.presentation.rest.error;

import java.net.URI;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.servifood.domain.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException exception) { return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ProblemDetail> validation(Exception exception) { return problem(HttpStatus.BAD_REQUEST, "Validation failed", "The request contains invalid values."); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail); problem.setTitle(title); problem.setType(URI.create("https://servifood.dev/problems/" + status.value()));
        return ResponseEntity.status(status).body(problem);
    }
}
