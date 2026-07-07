package com.hackathon.gares.dto;

import com.hackathon.gares.model.StatutPaiement;
import com.hackathon.gares.model.StatutReservation;

public record ReservationCompagnieDto(
        String id,
        String client,
        String trajet,
        String date,
        int tickets,
        int total,
        StatutReservation statut,
        StatutPaiement paiement
) {}
