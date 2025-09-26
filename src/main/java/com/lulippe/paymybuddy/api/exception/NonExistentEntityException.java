package com.lulippe.paymybuddy.api.exception;

public class NonExistentEntityException extends RuntimeException {
    public NonExistentEntityException(String message) {
        super(message);
    }
}
