package com.lulippe.paymybuddy.api.controller;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.InvalidDataException;
import com.lulippe.paymybuddy.api.exception.NonexistentEntityException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

import static com.lulippe.paymybuddy.utils.LogUtil.logRequestFailed;

@Hidden
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({EntityAlreadyExistsException.class, InsufficientFundsException.class})
    public ResponseEntity<String> handleEntityAlreadyExistsException(final Exception e) {
        logRequestFailed(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler({InvalidDataException.class, IllegalArgumentException.class})
    public ResponseEntity<String> handleBadRequestException(final Exception e) {
        logRequestFailed(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler({NonexistentEntityException.class})
    public ResponseEntity<String> handleNonexistentEntityException(final NonexistentEntityException e) {
        logRequestFailed(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(final MethodArgumentNotValidException ex) {
        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            if ("amount".equals(error.getField())) {
                errors.put("amount", "amount is mandatory and must be positive");
            } else if ("friendEmail".equals(error.getField())) {
                errors.put("friendEmail", "friendEmail is mandatory and must be a valid email address");
            } else {
                errors.put(error.getField(), error.getDefaultMessage());
            }
        });

        logRequestFailed("Validation failed: " + errors);

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String,String>> handleConstraintViolationException(final ConstraintViolationException e) {
        final Map<String, String> errors = new HashMap<>();
        e.getConstraintViolations().forEach(constraintViolation -> {
            final String field = constraintViolation.getPropertyPath().toString();

            errors.put(field, constraintViolation.getMessage());
        });
        logRequestFailed("Validation failed: " + errors);

        return ResponseEntity.badRequest().body(errors);
    }
}