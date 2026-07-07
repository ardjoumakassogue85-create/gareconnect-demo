package com.hackathon.gares.dto;

import com.hackathon.gares.model.MethodePaiement;
import com.hackathon.gares.model.OperateurMobileMoney;

public record ReservationRequest(
        String trajetId,
        MethodePaiement methodePaiement,
        String dateVoyage,
        Integer nombreTickets,
        OperateurMobileMoney operateurMobileMoney
) {}
