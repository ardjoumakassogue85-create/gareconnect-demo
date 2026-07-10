package com.hackathon.gares.dto;

import java.util.List;

public record VitrineRequest(
        String compagnie,
        String description,
        String logoUrl,
        String imageCouvertureUrl,
        List<String> galerieImages,
        String localisation,
        List<String> garesDesservies,
        List<String> flotte
) {}
