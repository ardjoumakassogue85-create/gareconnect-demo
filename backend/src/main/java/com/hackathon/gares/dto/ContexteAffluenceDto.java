package com.hackathon.gares.dto;

/**
 * Contexte du monde reel enrichi par l'IA : un facteur multiplicatif d'affluence
 * (1.0 = normal, >1 = jour charge, <1 = jour creux) et son explication.
 * `actif` est vrai quand le contexte modifie nettement l'affluence.
 */
public record ContexteAffluenceDto(
        double facteur,
        String raison,
        boolean actif
) {
}
