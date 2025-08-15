package com.lulippe.paymybuddy.mapper;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.entities.Transaction;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "friendName", source = "receiver.username")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "formatAmount")
    Transfer toTransfert (final Transaction transaction);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "sender", source = "sender")
    @Mapping(target = "receiver", source = "receiver")
    @Mapping(target = "amount", source = "transferAmount")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "executedAt", source = "executedAt")
    Transaction toTransaction (final AppUser sender, final AppUser receiver, final BigDecimal transferAmount, final String description, final Instant executedAt);

    @Named("formatAmount")
    static String formatAmount(final BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
