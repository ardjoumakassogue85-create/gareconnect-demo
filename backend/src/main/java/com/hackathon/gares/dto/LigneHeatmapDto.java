package com.hackathon.gares.dto;

import java.util.List;

/** Une ligne (un jour de la semaine) de la heatmap d'affluence compagnie. */
public record LigneHeatmapDto(
        String jour,
        List<CreneauAffluenceDto> creneaux
) {
}
