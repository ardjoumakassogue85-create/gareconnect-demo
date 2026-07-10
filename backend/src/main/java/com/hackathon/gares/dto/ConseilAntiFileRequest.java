package com.hackathon.gares.dto;

/**
 * Demande de conseil "anti-file d'attente".
 * Tous les champs sont optionnels : le mode proactif n'envoie que le contexte de
 * recherche (villes + date), le mode conversationnel ajoute une phrase libre.
 */
public record ConseilAntiFileRequest(
        String texteLibre,
        String villeDepart,
        String villeArrivee,
        String date
) {
}
