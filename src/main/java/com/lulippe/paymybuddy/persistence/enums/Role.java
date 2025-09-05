package com.lulippe.paymybuddy.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    ROLE_SYSTEM("ROLE_SYSTEM");

    private final String roleName;
}
