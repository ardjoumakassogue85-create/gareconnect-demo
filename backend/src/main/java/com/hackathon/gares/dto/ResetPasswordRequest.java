package com.hackathon.gares.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Le lien de reinitialisation est invalide")
        String token,
        @NotBlank
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password
) {
}
