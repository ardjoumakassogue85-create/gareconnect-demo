package com.hackathon.gares.dto;

import java.util.List;

/**
 * Conseil anti-file : un message clair, un trajet recommande, l'heure d'arrivee
 * conseillee, et des alternatives. Le champ `source` indique si le message vient
 * de l'IA ou du repli deterministe (transparence).
 */
public record ConseilAntiFileResponse(
        String message,
        String resume,
        TrajetDto trajetRecommande,
        String heureArrivee,
        String niveauAffluence,
        List<TrajetDto> alternatives,
        String source
) {
}
