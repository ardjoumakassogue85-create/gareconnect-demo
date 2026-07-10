package com.hackathon.gares.dto;

import java.util.List;

public record VitrineDto(
        String compagnie,
        String slug,
        String description,
        String logoUrl,
        String imageCouvertureUrl,
        List<String> galerieImages,
        String localisation,
        double noteMoyenne,
        int nombreAvis,
        List<String> garesDesservies,
        List<String> flotte
) {}
