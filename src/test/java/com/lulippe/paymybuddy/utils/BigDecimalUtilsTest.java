package com.lulippe.paymybuddy.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.lulippe.paymybuddy.utils.BigDecimalUtils.stringToBigDecimal;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BigDecimalUtilsTest {
    @Test
    @DisplayName("should throw IllegalArgumentException if null value")
    void shouldThrowIllegalArgumentExceptionIfNullValue() {
        //given
        final String value = null;

        //when & then
        assertThrows(IllegalArgumentException.class, () -> stringToBigDecimal(value));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException if value is empty")
    void shouldThrowIllegalArgumentExceptionIfValueIsEmpty() {
        //given
        final String value = "";

        //when & then
        assertThrows(IllegalArgumentException.class, () -> stringToBigDecimal(value));
    }

    @Test
    @DisplayName("should throw NumberFormatException if value is not a valid number")
    void shouldThrowNumberFormatExceptionIfValueIsNotANumber() {
        //given
        final String value = "test";

        //when & then
        assertThrows(NumberFormatException.class, () -> stringToBigDecimal(value));
    }

}