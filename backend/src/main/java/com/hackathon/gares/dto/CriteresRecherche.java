package com.hackathon.gares.dto;

import java.time.LocalDate;

public record CriteresRecherche(
        String villeDepart,
        String villeArrivee,
        LocalDate date,
        Integer budgetMax,
        String tri,
        Integer nombreResultats,
        String heureDepart,
        String compagnie,
        Integer prixMin,
        String statut
) {
    public static CriteresRecherche vide() {
        return new CriteresRecherche(null, null, null, null, null, null, null, null, null, null);
    }
}
