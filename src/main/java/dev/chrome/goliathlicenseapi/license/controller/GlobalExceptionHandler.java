package dev.chrome.goliathlicenseapi.license.controller;

import dev.chrome.goliathlicenseapi.license.exception.InvalidLicenseArgumentException;
import dev.chrome.goliathlicenseapi.license.exception.LicenseNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Invalid request.");
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), "INVALID_REQUEST", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Invalid request.");
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), "INVALID_REQUEST", message));
    }

    @ExceptionHandler(InvalidLicenseArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidLicenseArgument(InvalidLicenseArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), "INVALID_LICENSE", ex.getMessage()));
    }

    @ExceptionHandler(LicenseNotFoundException.class)
    public ResponseEntity<ApiError> handleLicenseNotFound(LicenseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), "LICENSE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), "INVALID_REQUEST", ex.getMessage()));
    }
}
