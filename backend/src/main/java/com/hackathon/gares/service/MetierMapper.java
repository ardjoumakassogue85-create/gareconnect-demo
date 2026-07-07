package com.hackathon.gares.service;

import com.hackathon.gares.dto.*;
import com.hackathon.gares.model.*;
import org.springframework.stereotype.Component;

@Component
public class MetierMapper {

    public VitrineDto toVitrineDto(CompagnieProfile compagnie) {
        return new VitrineDto(
                compagnie.getNom(),
                compagnie.getSlug(),
                compagnie.getDescription(),
                compagnie.getLogoUrl(),
                compagnie.getImageCouvertureUrl(),
                compagnie.getGalerieImages(),
                compagnie.getLocalisation(),
                compagnie.getNoteMoyenne(),
                compagnie.getNombreAvis(),
                compagnie.getGaresDesservies(),
                compagnie.getFlotte()
        );
    }

    public TrajetDto toTrajetDto(Trajet trajet) {
        return new TrajetDto(
                String.valueOf(trajet.getId()),
                trajet.getCompagnie().getNom(),
                trajet.getCodeGareDepart(),
                trajet.getCodeGareArrivee(),
                trajet.getVilleDepart(),
                trajet.getVilleArrivee(),
                trajet.getDate() == null ? null : trajet.getDate().toString(),
                trajet.getHeureDepart(),
                trajet.getDuree(),
                trajet.getPrix(),
                trajet.getPlacesDisponibles(),
                trajet.getStatut()
        );
    }

    public ReservationDto toReservationDto(Reservation reservation) {
        return new ReservationDto(
                String.valueOf(reservation.getId()),
                reservation.getTrajet() == null ? null : String.valueOf(reservation.getTrajet().getId()),
                reservation.getCodeBillet(),
                reservation.getCodeGareDepart(),
                reservation.getCodeGareArrivee(),
                reservation.getVilleDepart(),
                reservation.getVilleArrivee(),
                reservation.getDate() == null ? null : reservation.getDate().toString(),
                reservation.getHeure(),
                reservation.getCompagnie(),
                reservation.getPrix(),
                reservation.getNombreTickets(),
                reservation.getStatut(),
                reservation.getStatutPaiement(),
                reservation.getMethodePaiement(),
                reservation.getOperateurMobileMoney(),
                reservation.getCreeLe() == null ? null : reservation.getCreeLe().toString(),
                reservation.getAnnulableJusquA() == null ? null : reservation.getAnnulableJusquA().toString(),
                reservation.getNote(),
                reservation.getCommentaire()
        );
    }

   public ReclamationDto toReclamationDto(Reclamation reclamation) {
        return new ReclamationDto(
                String.valueOf(reclamation.getId()),
                reclamation.getClient() == null ? "Client" : reclamation.getClient().getNom(),
                reclamation.getSujet(),
                reclamation.getStatut(),
                reclamation.getMessages().stream().map(this::toMessageDto).toList(),
                reclamation.getCreeLe() == null ? null : reclamation.getCreeLe().toString(),
                reclamation.getMajLe() == null ? null : reclamation.getMajLe().toString()
        );
    }

    private MessageReclamationDto toMessageDto(MessageReclamation message) {
        return new MessageReclamationDto(
                String.valueOf(message.getId()),
                message.getAuteur(),
                message.getTexte(),
                message.getEnvoyeLe() == null ? null : message.getEnvoyeLe().toString()
        );
    }
}
