package com.hackathon.gares.dto;

import com.hackathon.gares.model.StatutReclamation;

import java.util.List;

public record ReclamationDto(
        String id,
        String sujet,
        StatutReclamation statut,
        List<MessageReclamationDto> messages,
        String creeLe,
        String majLe
) {}
