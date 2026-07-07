package com.hackathon.gares.dto;

import jakarta.validation.constraints.NotBlank;

public record AssistantRechercheRequest(
        @NotBlank String texteLibre,
        String villeDepart,
        String villeArrivee,
        String date
) {}
