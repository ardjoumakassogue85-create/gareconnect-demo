package com.hackathon.gares.dto;

public record TrajetRequest(
        String compagnie,
        String codeGareDepart,
        String codeGareArrivee,
        String villeDepart,
        String villeArrivee,
        String date,
        String heureDepart,
        String duree,
        int prix,
        int placesDisponibles
) {}
