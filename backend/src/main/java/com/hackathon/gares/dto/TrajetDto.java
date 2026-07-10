package com.hackathon.gares.dto;

import com.hackathon.gares.model.StatutTrajet;

public record TrajetDto(
        String id,
        String compagnie,
        String codeGareDepart,
        String codeGareArrivee,
        String villeDepart,
        String villeArrivee,
        String date,
        String heureDepart,
        String duree,
        int prix,
        int placesDisponibles,
        StatutTrajet statut
) {}
