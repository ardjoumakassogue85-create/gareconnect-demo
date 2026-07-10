package com.hackathon.gares.dto;

import com.hackathon.gares.model.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String nom,
        Role role,
        String expiresAt
) {}
