package com.hackathon.gares.dto;

import com.hackathon.gares.model.Role;

public record AccountResponse(
        Long id,
        String email,
        String nom,
        Role role,
        boolean emailVerified
) {
}
