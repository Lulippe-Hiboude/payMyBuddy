package com.lulippe.paymybuddy.api.exception;

public class InexistantEntityException extends RuntimeException {
    public InexistantEntityException(String message) {
        super(message);
    }
}
