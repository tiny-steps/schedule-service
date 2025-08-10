package com.tinysteps.scheduleservice.exception;

import com.tinysteps.scheduleservice.model.ErrorModel;
import com.tinysteps.scheduleservice.model.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseModel<Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ResponseModel<Object>> handleConflict(ResourceConflictException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseModel<Object>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseModel<Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ResponseModel<Object>> handleIntegrationError(IntegrationException ex) {
        log.error("Integration service error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseModel<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        // Collect all field errors
        List<ErrorModel> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErrorModel(
                        err.getDefaultMessage(),
                        "Field: " + err.getField(),
                        ZonedDateTime.now(),
                        null
                ))
                .collect(Collectors.toList());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseModel<Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private ResponseEntity<ResponseModel<Object>> buildErrorResponse(HttpStatus status, String message, List<ErrorModel> errors) {
        ResponseModel<Object> response =
                new ResponseModel<>(
                status.value(),
                status.name(),
                message,
                ZonedDateTime.now(),
                null,
                errors
        );
        return ResponseEntity.status(status).body(response);
    }
}
