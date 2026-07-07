package com.hackathon.gares.dto;

import com.hackathon.gares.model.MethodePaiement;
import com.hackathon.gares.model.OperateurMobileMoney;
import com.hackathon.gares.model.StatutPaiement;
import com.hackathon.gares.model.StatutReservation;

public record ReservationDto(
        String id,
        String trajetId,
        String codeBillet,
        String codeGareDepart,
        String codeGareArrivee,
        String villeDepart,
        String villeArrivee,
        String date,
        String heure,
        String compagnie,
        int prix,
        Integer nombreTickets,
        StatutReservation statut,
        StatutPaiement statutPaiement,
        MethodePaiement methodePaiement,
        OperateurMobileMoney operateurMobileMoney,
        String creeLe,
        String annulableJusquA,
        Integer note,
        String commentaire
) {}
