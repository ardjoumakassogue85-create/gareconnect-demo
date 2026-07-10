package com.hackathon.gares.dto;

/**
 * Resultat du controle d'un billet a l'embarquement.
 * valide=true : billet authentique et non encore utilise (embarquement autorise).
 * dejaUtilise=true : signature authentique mais billet deja scanne (fraude par copie).
 */
public record ValidationBilletResponse(
        boolean valide,
        String message,
        String passager,
        String trajet,
        String date,
        String heure,
        String compagnie,
        int places,
        boolean dejaUtilise,
        String valideLe
) {
}
