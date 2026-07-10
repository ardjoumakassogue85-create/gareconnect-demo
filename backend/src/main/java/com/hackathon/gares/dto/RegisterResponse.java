package com.hackathon.gares.dto;

public record RegisterResponse(
        String message,
        String email,
        boolean verificationRequise
) {}
