package com.lulippe.paymybuddy.api.exception;

public class NonexistentEntityException extends RuntimeException {
    public NonexistentEntityException(String message) {
        super(message);
    }
}
