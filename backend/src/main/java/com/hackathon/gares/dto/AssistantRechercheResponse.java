package com.hackathon.gares.dto;

import java.util.List;

public record AssistantRechercheResponse(
        CriteresRecherche criteresDetectes,
        List<TrajetDto> resultats,
        String message,
        boolean suggestions
) {}
