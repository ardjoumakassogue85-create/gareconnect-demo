package com.hackathon.gares.config;

import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.Reservation;
import com.hackathon.gares.model.Role;
import com.hackathon.gares.model.StatutPaiement;
import com.hackathon.gares.model.StatutReservation;
import com.hackathon.gares.model.StatutTrajet;
import com.hackathon.gares.model.Trajet;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.CompagnieProfileRepository;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.TrajetRepository;
import com.hackathon.gares.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Jeu de donnees de demonstration pour l'anti-file d'attente.
 *
 * Genere, une seule fois (idempotent via un email marqueur), quand
 * app.seed-demo=true (env SEED_DEMO) :
 *   - une compagnie et un client de demo ;
 *   - des trajets a des heures variees (pointe + creux) sur Abidjan <-> Bouake,
 *     pour voir des badges d'affluence FORTE / MOYENNE / FAIBLE dans la recherche ;
 *   - des reservations suivant une distribution horaire realiste sur les 7 derniers
 *     jours, pour que la courbe d'affluence et la heatmap soient reellement
 *     alimentees par des donnees (confiance elevee sur ces gares).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoAffluenceSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompagnieProfileRepository compagnieRepository;
    private final TrajetRepository trajetRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.seed-demo:false}")
    private boolean seedDemo;

    private static final String MARQUEUR = "demo-affluence@gareconnect.local";

    private static final String EMAIL_COMPAGNIE = "demo-compagnie@gareconnect.local";
    private static final String NOM_COMPAGNIE = "Ivoire Express Demo";

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemo) {
            return;
        }

        // Rafraichit les donnees demo a chaque demarrage : on nettoie l'ancien jeu
        // puis on regenere (ainsi les trajets restent toujours a des dates futures).
        nettoyerDonneesDemo();

        log.info("Generation des donnees demo d'affluence...");

        User client = userRepository.save(User.builder()
                .email(MARQUEUR)
                .passwordHash(passwordEncoder.encode("DemoPass123!"))
                .nom("Client Demo")
                .role(Role.CLIENT)
                .emailVerified(true)
                .build());

        CompagnieProfile compagnie = creerCompagnieDemo();

        int nbTrajets = creerTrajetsDemo(compagnie);
        int nbReservations = creerReservationsDemo(client);

        log.info("Seed demo termine : {} trajets et {} reservations generes.", nbTrajets, nbReservations);
    }

    private void nettoyerDonneesDemo() {
        // Ordre respectant les cles etrangeres : reservations + trajets + compagnie,
        // puis les users. On flush entre chaque etape : dans une meme transaction,
        // Hibernate execute sinon les INSERT avant les DELETE (collision d'email unique).
        userRepository.findByEmail(MARQUEUR).ifPresent(client ->
                reservationRepository.deleteAll(reservationRepository.findByClientOrderByCreeLeDesc(client)));

        compagnieRepository.findByNomIgnoreCase(NOM_COMPAGNIE).ifPresent(compagnie -> {
            trajetRepository.deleteAll(trajetRepository.findByCompagnieOrderByDateAscHeureDepartAsc(compagnie));
            compagnieRepository.delete(compagnie);
        });
        entityManager.flush();

        userRepository.findByEmail(MARQUEUR).ifPresent(userRepository::delete);
        userRepository.findByEmail(EMAIL_COMPAGNIE).ifPresent(userRepository::delete);
        entityManager.flush();
    }

    private CompagnieProfile creerCompagnieDemo() {
        User compagnieUser = userRepository.save(User.builder()
                .email(EMAIL_COMPAGNIE)
                .passwordHash(passwordEncoder.encode("DemoPass123!"))
                .nom(NOM_COMPAGNIE)
                .role(Role.COMPAGNIE)
                .emailVerified(true)
                .build());

        return compagnieRepository.save(CompagnieProfile.builder()
                .user(compagnieUser)
                .nom(NOM_COMPAGNIE)
                .slug("ivoire-express-demo")
                .description("Compagnie de demonstration pour l'intelligence d'affluence.")
                .build());
    }

    private int creerTrajetsDemo(CompagnieProfile compagnie) {
        // Heures choisies pour couvrir tout le spectre d'affluence (FORTE / MOYENNE / FAIBLE).
        String[] heures = {"06:30", "07:30", "12:00", "15:00", "17:30", "20:30"};
        int[] prix = {6000, 6500, 5500, 6000, 7000, 5200};
        // Villes ecrites avec les accents EXACTS du selecteur du frontend, sinon la
        // recherche (sensible aux accents) ne matcherait pas les trajets.
        // {villeDepart, villeArrivee, codeDepart, codeArrivee}
        String[][] routes = {
                {"Abidjan", "Bouaké", "ABJ", "BKE"}, {"Bouaké", "Abidjan", "BKE", "ABJ"},
                {"Abidjan", "Yamoussoukro", "ABJ", "YAM"}, {"Yamoussoukro", "Abidjan", "YAM", "ABJ"},
                {"Abidjan", "San-Pédro", "ABJ", "SPD"}, {"San-Pédro", "Abidjan", "SPD", "ABJ"},
                {"Abidjan", "Korhogo", "ABJ", "KOR"}, {"Korhogo", "Abidjan", "KOR", "ABJ"},
                {"Bouaké", "Korhogo", "BKE", "KOR"}, {"Korhogo", "Bouaké", "KOR", "BKE"},
        };

        LocalDate aujourdHui = LocalDate.now();
        List<Trajet> trajets = new ArrayList<>();
        for (int jour = 0; jour < 5; jour++) {
            LocalDate date = aujourdHui.plusDays(jour);
            for (String[] route : routes) {
                for (int i = 0; i < heures.length; i++) {
                    trajets.add(Trajet.builder()
                            .compagnie(compagnie)
                            .codeGareDepart(route[2])
                            .codeGareArrivee(route[3])
                            .villeDepart(route[0])
                            .villeArrivee(route[1])
                            .date(date)
                            .heureDepart(heures[i])
                            .duree("4h00")
                            .prix(prix[i])
                            .placesDisponibles(30)
                            .statut(StatutTrajet.ACTIF)
                            .build());
                }
            }
        }
        trajetRepository.saveAll(trajets);
        return trajets.size();
    }

    private int creerReservationsDemo(User client) {
        // Distribution horaire realiste et complete (05h->21h) : {heure, tickets/jour}.
        // Pics matin (07h/08h) et fin d'apres-midi (17h/18h), creux midi, calme tot/tard.
        int[][] distribution = {
                {5, 3}, {6, 10}, {7, 14}, {8, 13}, {9, 9}, {10, 7}, {11, 7}, {12, 9}, {13, 7},
                {14, 7}, {15, 9}, {16, 10}, {17, 13}, {18, 13}, {19, 9}, {20, 5}, {21, 3}
        };
        // Chaque gare de depart a une intensite propre => des profils d'affluence
        // distincts (les grandes villes sont plus chargees).
        // {villeDepart, villeArrivee, intensite en %}
        String[][] villes = {
                {"Abidjan", "Bouaké", "100"},
                {"Bouaké", "Abidjan", "95"},
                {"Yamoussoukro", "Abidjan", "90"},
                {"San-Pédro", "Abidjan", "85"},
                {"Korhogo", "Abidjan", "80"},
        };

        LocalDate aujourdHui = LocalDate.now();
        List<Reservation> reservations = new ArrayList<>();
        long compteur = 0;
        for (String[] ville : villes) {
            double intensite = Integer.parseInt(ville[2]) / 100.0;
            for (int jour = 1; jour <= 7; jour++) { // 7 jours => couvre tous les jours de la semaine
                LocalDate date = aujourdHui.minusDays(jour);
                for (int[] creneau : distribution) {
                    int tickets = Math.max(1, (int) Math.round(creneau[1] * intensite));
                    String heure = String.format("%02d:00", creneau[0]);
                    reservations.add(Reservation.builder()
                            .client(client)
                            .codeBillet("DEMO-" + (compteur++) + "-" + System.nanoTime())
                            .villeDepart(ville[0])
                            .villeArrivee(ville[1])
                            .date(date)
                            .heure(heure)
                            .compagnie(NOM_COMPAGNIE)
                            .prix(6000)
                            .nombreTickets(tickets)
                            .statut(StatutReservation.CONFIRMEE)
                            .statutPaiement(StatutPaiement.PAYE)
                            .creeLe(Instant.now())
                            .build());
                }
            }
        }
        reservationRepository.saveAll(reservations);
        return reservations.size();
    }
}
