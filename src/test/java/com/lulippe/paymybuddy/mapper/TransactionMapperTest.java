package com.lulippe.paymybuddy.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {
    @Test
    @DisplayName("should map to String")
    void shouldMapToString() {
        //given
        final BigDecimal amount = BigDecimal.valueOf(18.15683);

        //when
        final String amountString = TransactionMapper.formatAmount(amount);

        //then
        assertEquals("18.16", amountString);
    }
}