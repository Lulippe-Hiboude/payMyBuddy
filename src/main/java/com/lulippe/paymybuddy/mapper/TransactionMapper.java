package com.lulippe.paymybuddy.mapper;

import com.lulippe.paymybuddy.persistence.entities.Transaction;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "friendName", source = "receiver.username")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "formatAmount")
    Transfer toTransfert (final Transaction transaction);

    @Named("formatAmount")
    static String formatAmount(final BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
