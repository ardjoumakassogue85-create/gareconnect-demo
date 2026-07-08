package com.hackathon.gares.dto;

import java.util.List;

/**
 * Vue compagnie : heatmap de la demande (jours x creneaux) sur les gares
 * desservies, plus des suggestions d'ouverture de departs.
 */
public record AffluenceCompagnieDto(
        List<String> gares,
        List<String> creneaux,
        List<LigneHeatmapDto> heatmap,
        List<String> suggestions
) {
}
