package com.hackathon.gares.service;

import com.hackathon.gares.dto.AffluenceCompagnieDto;
import com.hackathon.gares.dto.AffluenceGareDto;
import com.hackathon.gares.dto.CreneauAffluenceDto;
import com.hackathon.gares.dto.LigneHeatmapDto;
import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.Reservation;
import com.hackathon.gares.model.StatutReservation;
import com.hackathon.gares.model.StatutTrajet;
import com.hackathon.gares.model.Trajet;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intelligence d'affluence "anti-file d'attente".
 *
 * Combine deux signaux pour estimer l'affluence en gare par creneau horaire :
 *   1. Le signal reel : les tickets deja reserves, agreges par gare x jour x heure.
 *   2. Un a priori metier : la courbe type des departs interurbains (pointes du
 *      matin et de fin d'apres-midi), ponderee par le jour de la semaine.
 *
 * Le melange est adaptatif : quand une gare a peu d'historique, l'a priori domine
 * (affichage credible des le premier jour) ; a mesure que les reservations
 * s'accumulent, le signal reel prend le dessus. C'est la promesse du cahier des
 * charges : "la collecte demarre, le modele affine".
 */
@Service
@RequiredArgsConstructor
public class AffluenceService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;

    private static final int HEURE_DEBUT = 5;
    private static final int HEURE_FIN = 21;
    private static final double TICKETS_REF = 18.0;      // tickets/heure => saturation du signal reel
    private static final double SEUIL_CONFIANCE = 40.0;  // total tickets gare => confiance max dans le reel

    // Minutes conseillees pour arriver avant le depart selon le niveau d'affluence.
    private static final int MARGE_FAIBLE = 20;
    private static final int MARGE_MOYENNE = 35;
    private static final int MARGE_FORTE = 50;

    // Courbe type des departs interurbains (0..100), indexee par heure.
    private static final double[] PRIOR = new double[24];
    static {
        Arrays.fill(PRIOR, 12);
        PRIOR[5] = 55; PRIOR[6] = 85; PRIOR[7] = 100; PRIOR[8] = 92; PRIOR[9] = 72; PRIOR[10] = 55;
        PRIOR[11] = 48; PRIOR[12] = 55; PRIOR[13] = 50; PRIOR[14] = 47; PRIOR[15] = 55; PRIOR[16] = 72;
        PRIOR[17] = 88; PRIOR[18] = 94; PRIOR[19] = 76; PRIOR[20] = 52; PRIOR[21] = 38;
    }

    private static final String[] JOURS_FR = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    // -- Cote voyageur : profil d'une gare pour une journee --

    @Transactional(readOnly = true)
    public AffluenceGareDto affluenceGare(String ville, LocalDate date) {
        LocalDate jour = date == null ? LocalDate.now() : date;
        ProfilAffluence profil = chargerProfil(List.of(ville == null ? "" : ville));
        DayOfWeek dow = jour.getDayOfWeek();

        List<CreneauAffluenceDto> creneaux = new ArrayList<>();
        int scoreMax = -1, scoreMin = 101, heureMax = HEURE_DEBUT, heureMin = HEURE_DEBUT, somme = 0;
        for (int h = HEURE_DEBUT; h <= HEURE_FIN; h++) {
            int s = profil.score(dow, h);
            creneaux.add(new CreneauAffluenceDto(formatHeure(h), s, niveau(s)));
            somme += s;
            if (s > scoreMax) { scoreMax = s; heureMax = h; }
            if (s < scoreMin) { scoreMin = s; heureMin = h; }
        }
        int moyenne = creneaux.isEmpty() ? 0 : somme / creneaux.size();

        return new AffluenceGareDto(
                libelleGare(ville),
                jour.toString(),
                libelleJour(dow),
                niveau(moyenne),
                formatHeure(heureMax),
                formatHeure(heureMin),
                (int) Math.round(profil.confiance() * 100),
                creneaux
        );
    }

    // -- Cote compagnie : heatmap de la demande + suggestions --

    @Transactional(readOnly = true)
    public AffluenceCompagnieDto affluenceCompagnie(CompagnieProfile compagnie) {
        List<Trajet> trajets = trajetRepository.findByCompagnieOrderByDateAscHeureDepartAsc(compagnie);

        Set<String> villes = new LinkedHashSet<>();
        for (Trajet t : trajets) {
            if (t.getVilleDepart() != null && !t.getVilleDepart().isBlank()) {
                villes.add(t.getVilleDepart().trim());
            }
        }

        ProfilAffluence profil = chargerProfil(villes);

        // Creneaux de 2h pour une grille lisible.
        int[] departs = {5, 7, 9, 11, 13, 15, 17, 19};
        List<String> libellesCreneaux = new ArrayList<>();
        for (int d : departs) {
            libellesCreneaux.add(formatHeure(d) + "-" + formatHeure(d + 2));
        }

        // Offre actuelle (departs ACTIFS non expires) par (jour, creneau).
        int[][] offre = new int[7][departs.length];
        for (Trajet t : trajets) {
            if (t.getStatut() != StatutTrajet.ACTIF || t.estExpire() || t.getDate() == null) {
                continue;
            }
            Integer h = heureDe(t.getHeureDepart());
            if (h == null) continue;
            int c = indexCreneau(h, departs);
            if (c < 0) continue;
            offre[t.getDate().getDayOfWeek().getValue() - 1][c]++;
        }

        List<LigneHeatmapDto> heatmap = new ArrayList<>();
        List<int[]> pointsChauds = new ArrayList<>(); // {jourIndex, creneauIndex, score}
        for (DayOfWeek dow : DayOfWeek.values()) {
            List<CreneauAffluenceDto> ligne = new ArrayList<>();
            for (int c = 0; c < departs.length; c++) {
                int h = departs[c];
                int score = (profil.score(dow, h) + profil.score(dow, h + 1)) / 2;
                ligne.add(new CreneauAffluenceDto(libellesCreneaux.get(c), score, niveau(score)));
                pointsChauds.add(new int[]{dow.getValue() - 1, c, score});
            }
            heatmap.add(new LigneHeatmapDto(libelleJour(dow), ligne));
        }

        List<String> suggestions = construireSuggestions(pointsChauds, offre, libellesCreneaux, villes.isEmpty());

        return new AffluenceCompagnieDto(
                new ArrayList<>(villes),
                libellesCreneaux,
                heatmap,
                suggestions
        );
    }

    /** Conseil "meilleur moment pour arriver" pour un depart donne. */
    public String conseilArrivee(String heureDepart, String niveau) {
        Integer minutes = minutesDe(heureDepart);
        if (minutes == null) return null;
        int marge = switch (niveau == null ? "" : niveau) {
            case "FORTE" -> MARGE_FORTE;
            case "MOYENNE" -> MARGE_MOYENNE;
            default -> MARGE_FAIBLE;
        };
        int arrivee = Math.max(0, minutes - marge);
        return String.format("%02d:%02d", arrivee / 60, arrivee % 60);
    }

    // -- Interne --

    private List<String> construireSuggestions(List<int[]> pointsChauds, int[][] offre,
                                               List<String> libellesCreneaux, boolean sansTrajets) {
        if (sansTrajets) {
            return List.of("Ajoute des trajets pour obtenir des recommandations d'affluence basees sur tes gares.");
        }
        // Trie les creneaux par demande decroissante.
        pointsChauds.sort((a, b) -> Integer.compare(b[2], a[2]));
        List<String> suggestions = new ArrayList<>();
        for (int[] p : pointsChauds) {
            int jour = p[0], creneau = p[1], score = p[2];
            if (score < 60) break;                 // on ne suggere que la vraie demande
            if (offre[jour][creneau] > 0) continue; // deja couvert
            suggestions.add(String.format(
                    "Forte demande %s sur le creneau %s (affluence %d%%) et aucun de tes departs : envisage d'en ajouter un.",
                    JOURS_FR[jour], libellesCreneaux.get(creneau), score));
            if (suggestions.size() >= 3) break;
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Ton offre couvre bien les creneaux de forte demande. Continue le suivi.");
        }
        return suggestions;
    }

    private ProfilAffluence chargerProfil(Iterable<String> villes) {
        Map<Integer, Integer> tickets = new HashMap<>();
        int total = 0;
        for (String ville : villes) {
            if (ville == null || ville.isBlank()) continue;
            for (Reservation r : reservationRepository
                    .findByVilleDepartIgnoreCaseAndStatut(ville.trim(), StatutReservation.CONFIRMEE)) {
                Integer h = heureDe(r.getHeure());
                if (r.getDate() == null || h == null) continue;
                int cle = r.getDate().getDayOfWeek().getValue() * 100 + h;
                int n = Math.max(1, r.getNombreTickets());
                tickets.merge(cle, n, Integer::sum);
                total += n;
            }
        }
        return new ProfilAffluence(tickets, total);
    }

    /** Profil pre-agrege exposant un score 0..100 par (jour, heure). */
    private static final class ProfilAffluence {
        private final Map<Integer, Integer> tickets;
        private final double alpha;

        ProfilAffluence(Map<Integer, Integer> tickets, int total) {
            this.tickets = tickets;
            this.alpha = Math.min(1.0, total / SEUIL_CONFIANCE);
        }

        double confiance() {
            return alpha;
        }

        int score(DayOfWeek dow, int heure) {
            if (heure < 0 || heure > 23) return 0;
            double prior = Math.min(100.0, PRIOR[heure] * multiplicateurJour(dow));
            int t = tickets.getOrDefault(dow.getValue() * 100 + heure, 0);
            double reel = Math.min(100.0, 100.0 * t / TICKETS_REF);
            double score = (1 - alpha) * prior + alpha * reel;
            return (int) Math.round(Math.max(0, Math.min(100, score)));
        }
    }

    private static double multiplicateurJour(DayOfWeek dow) {
        return switch (dow) {
            case FRIDAY -> 1.15;
            case SUNDAY -> 1.18;
            case SATURDAY -> 1.05;
            default -> 1.0;
        };
    }

    static String niveau(int score) {
        if (score < 34) return "FAIBLE";
        if (score < 67) return "MOYENNE";
        return "FORTE";
    }

    private static int indexCreneau(int heure, int[] departs) {
        for (int i = 0; i < departs.length; i++) {
            if (heure >= departs[i] && heure < departs[i] + 2) return i;
        }
        return -1;
    }

    private static Integer heureDe(String heure) {
        Integer minutes = minutesDe(heure);
        return minutes == null ? null : minutes / 60;
    }

    private static Integer minutesDe(String heure) {
        if (heure == null || heure.isBlank()) return null;
        try {
            String[] parts = heure.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return h * 60 + m;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String formatHeure(int heure) {
        return String.format("%02d:00", Math.max(0, Math.min(23, heure)));
    }

    private static String libelleJour(DayOfWeek dow) {
        return JOURS_FR[dow.getValue() - 1];
    }

    private static String libelleGare(String ville) {
        String v = ville == null || ville.isBlank() ? "principale" : ville.trim();
        return "Gare de " + v;
    }
}
