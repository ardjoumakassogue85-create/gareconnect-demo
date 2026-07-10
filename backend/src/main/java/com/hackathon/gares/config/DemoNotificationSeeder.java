package com.hackathon.gares.config;

import com.hackathon.gares.model.Reservation;
import com.hackathon.gares.model.StatutPaiement;
import com.hackathon.gares.model.StatutReservation;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Pour rendre la notification de notation demontrable sans attendre 24h : cree,
 * une seule fois, deux voyages DEJA TERMINES et non notes pour le client de demo.
 * Le job planifie les transformera en rappels "Note ton voyage".
 *
 * Idempotent (via un code de billet marqueur) et sans effet si le client de demo
 * n'existe pas encore.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoNotificationSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    private static final String CLIENT_DEMO = "demo-affluence@gareconnect.local";
    private static final String MARQUEUR = "GC-DEMO-VOYAGE-1";

    @Override
    @Transactional
    public void run(String... args) {
        User client = userRepository.findByEmail(CLIENT_DEMO).orElse(null);
        if (client == null || reservationRepository.existsByCodeBillet(MARQUEUR)) {
            return;
        }

        reservationRepository.saveAll(List.of(
                voyageTermine(client, MARQUEUR, "Abidjan", "Bouaké",
                        LocalDate.now().minusDays(2), "07:30", "Ivoire Express Demo", 6500),
                voyageTermine(client, "GC-DEMO-VOYAGE-2", "Abidjan", "Yamoussoukro",
                        LocalDate.now().minusDays(3), "12:00", "Ivoire Express Demo", 5500)
        ));
        log.info("Voyages demo a noter crees pour le client de demonstration.");
    }

    private Reservation voyageTermine(User client, String codeBillet, String depart, String arrivee,
                                      LocalDate date, String heure, String compagnie, int prix) {
        return Reservation.builder()
                .client(client)
                .codeBillet(codeBillet)
                .villeDepart(depart)
                .villeArrivee(arrivee)
                .date(date)
                .heure(heure)
                .compagnie(compagnie)
                .prix(prix)
                .nombreTickets(1)
                .statut(StatutReservation.CONFIRMEE)
                .statutPaiement(StatutPaiement.PAYE)
                .creeLe(Instant.now().minus(Duration.ofDays(5)))
                .build();
    }
}
