package com.example.chatapp.security;

import java.security.Principal;

public record CustomUserPrincipal(String id, String uid, String gmail, String name, String role) implements Principal {
    @Override
    public String getName() {
        return id;
    }
}
