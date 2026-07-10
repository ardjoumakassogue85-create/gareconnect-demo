package com.hackathon.gares.dto;

/**
 * "Pass coupe-file" : la fenetre d'arrivee conseillee au voyageur.
 */
public record CreneauArriveeResponse(
        String gare,
        String dateVoyage,
        String heureDepart,
        String fenetreDebut,
        String fenetreFin,
        int position,
        String message,
        boolean dejaAttribue
) {
}
