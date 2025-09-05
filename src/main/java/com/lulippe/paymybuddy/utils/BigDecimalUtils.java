package com.lulippe.paymybuddy.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class BigDecimalUtils {
    public static BigDecimal stringToBigDecimal(final String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value cannot be null or empty");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Value is not a valid number : " + value);
        }
    }
}
