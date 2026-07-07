package com.hackathon.gares.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(
        @NotBlank(message = "Le nom est obligatoire")
        String nom,
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email est invalide")
        String email,
        // Optionnels : uniquement pour changer de mot de passe.
        String motDePasseActuel,
        String nouveauMotDePasse
) {
}
