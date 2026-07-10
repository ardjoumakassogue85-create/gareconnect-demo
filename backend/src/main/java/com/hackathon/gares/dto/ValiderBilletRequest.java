package com.hackathon.gares.dto;

import jakarta.validation.constraints.NotBlank;

public record ValiderBilletRequest(
        @NotBlank String token
) {
}
