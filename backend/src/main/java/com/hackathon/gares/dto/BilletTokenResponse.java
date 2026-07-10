package com.hackathon.gares.dto;

/** Jeton signe encode dans le QR code du billet. */
public record BilletTokenResponse(
        String token,
        String codeBillet
) {
}
