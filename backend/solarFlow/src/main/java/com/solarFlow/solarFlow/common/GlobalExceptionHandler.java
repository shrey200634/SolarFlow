package com.solarFlow.solarFlow.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleStatus(ResponseStatusException ex) {
        HttpStatus s = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(s)
                .body(new ApiError(Instant.now(), s.value(), s.name(), ex.getReason(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "VALIDATION_FAILED", "Invalid request", fields));
    }

    // Race condition: two parallel registers pass existsByEmail, DB unique index catches the second
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDb(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError(Instant.now(), 409, "CONFLICT", "Resource already exists", null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413).body(
                new ApiError(Instant.now(), 413, "PAYLOAD_TOO_LARGE", "File must be 8 MB or smaller", null));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "VALIDATION_FAILED", "Invalid request",
                        Map.of("file", "is required")));
    }

    // @PreAuthorize failures: JSON body instead of an empty 403
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiError(Instant.now(), 403, "FORBIDDEN", "You don't have permission to do this", null));
    }

    // Malformed JSON or an invalid enum/date value inside a body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "BAD_REQUEST", "Request body is missing or malformed", null));
    }

    // Invalid enum/UUID in a query string or path, e.g. ?status=FOO
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "BAD_REQUEST", "Invalid value for '" + ex.getName() + "'", null));
    }
}