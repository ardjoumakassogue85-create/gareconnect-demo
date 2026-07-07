package com.hackathon.gares.service;

import com.hackathon.gares.dto.AvisRequest;
import com.hackathon.gares.dto.ReservationDto;
import com.hackathon.gares.dto.ReservationRequest;
import com.hackathon.gares.dto.TrajetDto;
import com.hackathon.gares.model.*;
import com.hackathon.gares.repository.RechercheLogRepository;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.TrajetRepository;
import com.hackathon.gares.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoyageurService {

    private static final Duration DELAI_ANNULATION = Duration.ofMinutes(30);

    private final TrajetRepository trajetRepository;
    private final ReservationRepository reservationRepository;
    private final RechercheLogRepository rechercheLogRepository;
    private final UserRepository userRepository;
    private final MetierMapper mapper;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public List<TrajetDto> rechercher(String villeDepart, String villeArrivee, String date) {
        rechercheLogRepository.save(RechercheLog.builder()
                .villeDepart(villeDepart)
                .villeArrivee(villeArrivee)
                .dateRecherche(date)
                .creeLe(Instant.now())
                .build());

        LocalDate jour = date == null || date.isBlank() ? null : LocalDate.parse(date);
        List<Trajet> trajets;
        if (villeDepart != null && villeArrivee != null && jour != null
                && !villeDepart.isBlank() && !villeArrivee.isBlank()) {
            trajets = trajetRepository.findByStatutAndVilleDepartIgnoreCaseAndVilleArriveeIgnoreCaseAndDateOrderByHeureDepartAsc(
                    StatutTrajet.ACTIF,
                    villeDepart.trim(),
                    villeArrivee.trim(),
                    jour
            );
        } else {
            trajets = trajetRepository.findByStatutOrderByDateAscHeureDepartAsc(StatutTrajet.ACTIF).stream()
                    .filter(t -> villeDepart == null || villeDepart.isBlank() || t.getVilleDepart().equalsIgnoreCase(villeDepart.trim()))
                    .filter(t -> villeArrivee == null || villeArrivee.isBlank() || t.getVilleArrivee().equalsIgnoreCase(villeArrivee.trim()))
                    .filter(t -> jour == null || jour.equals(t.getDate()))
                    .toList();
        }
        // Les trajets deja partis restent en base mais ne sont plus proposes a la reservation.
        return trajets.stream()
                .filter(t -> !t.estExpire())
                .map(mapper::toTrajetDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrajetDto obtenirTrajet(Long id) {
        Trajet trajet = trajetRepository.findById(id)
                .filter(t -> t.getStatut() == StatutTrajet.ACTIF)
                .filter(t -> !t.estExpire())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable"));
        return mapper.toTrajetDto(trajet);
    }

    @Transactional
    public ReservationDto creerReservation(Authentication authentication, ReservationRequest request) {
        User client = getUser(authentication);
        Trajet trajet = trajetRepository.findById(Long.parseLong(request.trajetId()))
                .filter(t -> t.getStatut() == StatutTrajet.ACTIF)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable"));

        if (trajet.estExpire()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce trajet est deja parti");
        }

        int nombreTickets = request.nombreTickets() == null || request.nombreTickets() < 1 ? 1 : request.nombreTickets();
        if (trajet.getPlacesDisponibles() < nombreTickets) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Places insuffisantes");
        }

        trajet.setPlacesDisponibles(trajet.getPlacesDisponibles() - nombreTickets);
        Instant maintenant = Instant.now();
        Reservation reservation = Reservation.builder()
                .client(client)
                .trajet(trajet)
                .codeBillet(genererCodeBillet())
                .codeGareDepart(trajet.getCodeGareDepart())
                .codeGareArrivee(trajet.getCodeGareArrivee())
                .villeDepart(trajet.getVilleDepart())
                .villeArrivee(trajet.getVilleArrivee())
                .date(request.dateVoyage() == null || request.dateVoyage().isBlank() ? trajet.getDate() : LocalDate.parse(request.dateVoyage()))
                .heure(trajet.getHeureDepart())
                .compagnie(trajet.getCompagnie().getNom())
                .prix(trajet.getPrix() * nombreTickets)
                .nombreTickets(nombreTickets)
                .statut(StatutReservation.CONFIRMEE)
                .statutPaiement(StatutPaiement.PAYE)
                .methodePaiement(request.methodePaiement())
                .operateurMobileMoney(request.operateurMobileMoney())
                .creeLe(maintenant)
                .annulableJusquA(maintenant.plus(DELAI_ANNULATION))
                .build();
        return mapper.toReservationDto(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> listerMesReservations(Authentication authentication) {
        return reservationRepository.findByClientOrderByCreeLeDesc(getUser(authentication)).stream()
                .map(mapper::toReservationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationDto obtenirReservation(Authentication authentication, Long id) {
        return mapper.toReservationDto(reservationDuClient(authentication, id));
    }

    @Transactional
    public ReservationDto annulerReservation(Authentication authentication, Long id) {
        Reservation reservation = reservationDuClient(authentication, id);
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            return mapper.toReservationDto(reservation);
        }
        reservation.setStatut(StatutReservation.ANNULEE);
        reservation.setStatutPaiement(StatutPaiement.REMBOURSE);
        if (reservation.getTrajet() != null) {
            reservation.getTrajet().setPlacesDisponibles(
                    reservation.getTrajet().getPlacesDisponibles() + reservation.getNombreTickets()
            );
        }
        return mapper.toReservationDto(reservation);
    }

    @Transactional
    public ReservationDto laisserAvis(Authentication authentication, Long reservationId, AvisRequest request) {
        Reservation reservation = reservationDuClient(authentication, reservationId);
        int note = Math.max(1, Math.min(5, request.note()));
        reservation.setNote(note);
        reservation.setCommentaire(request.commentaire());
        return mapper.toReservationDto(reservation);
    }

    private Reservation reservationDuClient(Authentication authentication, Long id) {
        User client = getUser(authentication);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable"));
        if (!reservation.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reservation inaccessible");
        }
        return reservation;
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private String genererCodeBillet() {
        return "GC-" + Instant.now().getEpochSecond() + "-" + (1000 + random.nextInt(9000));
    }
}
