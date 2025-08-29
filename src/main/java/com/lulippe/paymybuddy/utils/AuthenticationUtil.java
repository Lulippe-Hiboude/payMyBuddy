package com.lulippe.paymybuddy.utils;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class AuthenticationUtil {
    public static String getAuthenticatedUserEmail(){
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
