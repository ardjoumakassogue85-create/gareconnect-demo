package com.hackathon.gares.dto;

/**
 * Affluence prevue pour un creneau horaire.
 * niveau : FAIBLE | MOYENNE | FORTE ; score : 0..100.
 */
public record CreneauAffluenceDto(
        String heure,
        int score,
        String niveau
) {
}
