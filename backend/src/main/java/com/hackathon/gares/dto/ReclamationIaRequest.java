package com.hackathon.gares.dto;

import java.util.List;

public record ReclamationIaRequest(
        String reclamationId,
        String clientNom,
        String clientEmail,
        String sujet,
        String statutActuel,
        String message,
        List<HistoriqueMessage> historique
) {
    public record HistoriqueMessage(String auteur, String texte, String envoyeLe) {}
}