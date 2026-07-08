package com.hackathon.gares.dto;

import java.util.List;

/**
 * Profil d'affluence d'une gare pour une journee donnee.
 * Cote voyageur : sert a afficher l'indicateur d'affluence et le conseil d'arrivee.
 */
public record AffluenceGareDto(
        String gare,
        String date,
        String jour,
        String niveauGlobal,
        String heureLaPlusChargee,
        String creneauLePlusCalme,
        int confiance,
        List<CreneauAffluenceDto> creneaux
) {
}
