package com.hackathon.gares.service;

import com.hackathon.gares.dto.NotificationDto;
import com.hackathon.gares.model.Notification;
import com.hackathon.gares.model.Reservation;
import com.hackathon.gares.model.StatutReservation;
import com.hackathon.gares.model.TypeNotification;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.NotificationRepository;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Notifications voyageur.
 *
 * Fonctionnalite phare : 24h apres un voyage, on cree automatiquement une
 * notification invitant le client a noter la compagnie sur 5. Un job planifie
 * balaie periodiquement les reservations confirmees non notees dont le depart
 * remonte a plus de 24h, et cree le rappel (une seule fois par reservation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Value("${app.notification.delai-heures:24}")
    private long delaiHeures;

    /**
     * Genere les rappels de notation. Premier passage 20s apres le demarrage,
     * puis toutes les 5 min par defaut.
     */
    @Scheduled(initialDelayString = "20000", fixedDelayString = "${app.notification.intervalle-ms:300000}")
    @Transactional
    public void genererRappelsNotation() {
        List<Reservation> candidates = reservationRepository.findByStatutAndNoteIsNull(StatutReservation.CONFIRMEE);
        int crees = 0;
        for (Reservation reservation : candidates) {
            if (!estEligible(reservation)) {
                continue;
            }
            if (notificationRepository.existsByReservationIdAndType(reservation.getId(), TypeNotification.NOTER_VOYAGE)) {
                continue;
            }
            notificationRepository.save(construireRappel(reservation));
            crees++;
        }
        if (crees > 0) {
            log.info("Notifications de notation creees : {}", crees);
        }
    }

    private boolean estEligible(Reservation reservation) {
        // On ignore les reservations de demonstration agregees (donnees d'affluence).
        if (reservation.getCodeBillet() != null && reservation.getCodeBillet().startsWith("DEMO-")) {
            return false;
        }
        if (reservation.getClient() == null || reservation.getDate() == null) {
            return false;
        }
        LocalDateTime depart = LocalDateTime.of(reservation.getDate(), parseHeure(reservation.getHeure()));
        return depart.plusHours(delaiHeures).isBefore(LocalDateTime.now());
    }

    private Notification construireRappel(Reservation reservation) {
        String trajet = reservation.getVilleDepart() + " → " + reservation.getVilleArrivee();
        return Notification.builder()
                .user(reservation.getClient())
                .type(TypeNotification.NOTER_VOYAGE)
                .titre("Note ton voyage")
                .message("Comment s'est passé ton trajet " + trajet + " avec " + reservation.getCompagnie()
                        + " ? Donne une note sur 5 pour aider les autres voyageurs.")
                .reservationId(reservation.getId())
                .compagnie(reservation.getCompagnie())
                .lu(false)
                .creeLe(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> lister(Authentication authentication) {
        return notificationRepository.findByUserOrderByCreeLeDesc(getUser(authentication)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Authentication authentication) {
        return notificationRepository.countByUserAndLuFalse(getUser(authentication));
    }

    @Transactional
    public void marquerLu(Authentication authentication, Long id) {
        User user = getUser(authentication);
        notificationRepository.findByIdAndUser(id, user).ifPresent(notification -> {
            notification.setLu(true);
            notificationRepository.save(notification);
        });
    }

    /** Marque comme lue la notification de notation liee a une reservation (apres notation). */
    @Transactional
    public void marquerNotationFaite(Long reservationId) {
        notificationRepository.findByReservationIdAndType(reservationId, TypeNotification.NOTER_VOYAGE)
                .ifPresent(notification -> {
                    notification.setLu(true);
                    notificationRepository.save(notification);
                });
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                String.valueOf(notification.getId()),
                notification.getType().name(),
                notification.getTitre(),
                notification.getMessage(),
                notification.getReservationId() == null ? null : String.valueOf(notification.getReservationId()),
                notification.getCompagnie(),
                notification.isLu(),
                notification.getCreeLe() == null ? null : notification.getCreeLe().toString()
        );
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private static LocalTime parseHeure(String heure) {
        if (heure == null || heure.isBlank()) {
            return LocalTime.MIDNIGHT;
        }
        try {
            return LocalTime.parse(heure.trim());
        } catch (Exception ignore) {
            return LocalTime.MIDNIGHT;
        }
    }
}
