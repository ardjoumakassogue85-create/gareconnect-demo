package com.hackathon.gares.dto;

import jakarta.validation.constraints.NotNull;

public record CreneauArriveeRequest(
        @NotNull Long trajetId
) {
}
