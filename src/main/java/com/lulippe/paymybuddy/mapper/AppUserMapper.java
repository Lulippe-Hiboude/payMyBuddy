package com.lulippe.paymybuddy.mapper;

import com.lulippe.paymybuddy.bankTransfer.model.BankTransferResponse;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.enums.Role;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AppUserMapper {
    AppUserMapper INSTANCE = Mappers.getMapper(AppUserMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "hashedPassword")
    @Mapping(target = "role", source = "roleEnum", qualifiedByName = "mapRoleEnumToRole")
    @Mapping(target = "userId", ignore = true)
    AppUser ToAppUser(final String username, final String email, final String hashedPassword, final RegisterRequest.RoleEnum roleEnum);

    @Mapping(target = "receiver", source = "username")
    @Mapping(target = "amount", source = "account")
    BankTransferResponse ToBankTransferResponse(final AppUser appUser);

    @Named("mapRoleEnumToRole")
    default Role mapRoleEnumToRole(final RegisterRequest.RoleEnum roleEnum) {
        if (roleEnum == null) {
            throw new IllegalArgumentException("roleEnum cannot be null");
        }
        return Role.valueOf(roleEnum.name());
    }
}
