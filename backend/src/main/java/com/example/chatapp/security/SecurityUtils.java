package com.example.chatapp.security;

import com.example.chatapp.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static CustomUserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new ForbiddenException("Authentication required");
        }
        return principal;
    }

    public static String currentUserId() {
        return currentUser().id();
    }
}
