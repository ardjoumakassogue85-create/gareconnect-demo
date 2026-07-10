package com.hackathon.gares.service;

import com.hackathon.gares.dto.AffluenceGareDto;
import com.hackathon.gares.dto.CreneauArriveeResponse;
import com.hackathon.gares.model.CreneauArrivee;
import com.hackathon.gares.model.Trajet;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.CreneauArriveeRepository;
import com.hackathon.gares.repository.TrajetRepository;
import com.hackathon.gares.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;

/**
 * File virtuelle "coupe-file".
 *
 * Au lieu de simplement informer, on AGIT sur la file : on attribue a chaque
 * voyageur une fenetre d'arrivee de 15 min, en choisissant la moins chargee dans
 * l'heure precedant son depart. La charge d'une fenetre = l'affluence prevue en
 * gare + le nombre de voyageurs deja places sur cette fenetre. Resultat : les
 * arrivees s'etalent, le pic est ecrete, la file raccourcit pour tout le monde.
 */
@Service
@RequiredArgsConstructor
public class FileVirtuelleService {

    private final TrajetRepository trajetRepository;
    private final UserRepository userRepository;
    private final CreneauArriveeRepository creneauRepository;
    private final AffluenceService affluenceService;

    // Chaque voyageur deja place sur une fenetre pese ce nombre de points face a l'affluence.
    private static final int POIDS_OCCUPATION = 8;
    private static final int FENETRE_MINUTES = 15;

    @Transactional
    public CreneauArriveeResponse attribuer(Authentication authentication, Long trajetId) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        Trajet trajet = trajetRepository.findById(trajetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable"));

        String gare = trajet.getVilleDepart();
        LocalDate date = trajet.getDate();
        Integer minutesDepart = minutes(trajet.getHeureDepart());
        if (minutesDepart == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Heure de depart invalide");
        }

        // Idempotent : si le voyageur a deja un creneau pour ce depart, on le renvoie.
        CreneauArrivee existant = creneauRepository.findByUserAndTrajetId(user, trajetId).orElse(null);
        if (existant != null) {
            long position = creneauRepository.countByGareIgnoreCaseAndDateVoyageAndFenetre(
                    gare, date, existant.getFenetre());
            return reponse(trajet, existant.getFenetre(), (int) position, true);
        }

        AffluenceGareDto profil = (gare == null || gare.isBlank()) ? null : affluenceService.affluenceGare(gare, date);

        // On cherche la fenetre la moins chargee dans [depart-60, depart-15].
        String meilleureFenetre = null;
        double meilleureCharge = Double.MAX_VALUE;
        long occupationRetenue = 0;
        for (int minute = minutesDepart - 60; minute <= minutesDepart - FENETRE_MINUTES; minute += FENETRE_MINUTES) {
            if (minute < 0) {
                continue;
            }
            String fenetre = format(minute);
            double affluence = scoreA(profil, minute / 60);
            long occupation = creneauRepository.countByGareIgnoreCaseAndDateVoyageAndFenetre(gare, date, fenetre);
            double charge = affluence + occupation * POIDS_OCCUPATION;
            if (charge < meilleureCharge) {
                meilleureCharge = charge;
                meilleureFenetre = fenetre;
                occupationRetenue = occupation;
            }
        }

        if (meilleureFenetre == null) {
            meilleureFenetre = format(Math.max(0, minutesDepart - 20));
            occupationRetenue = creneauRepository.countByGareIgnoreCaseAndDateVoyageAndFenetre(gare, date, meilleureFenetre);
        }

        creneauRepository.save(CreneauArrivee.builder()
                .user(user)
                .gare(gare)
                .dateVoyage(date)
                .fenetre(meilleureFenetre)
                .trajetId(trajetId)
                .creeLe(Instant.now())
                .build());

        return reponse(trajet, meilleureFenetre, (int) (occupationRetenue + 1), false);
    }

    private CreneauArriveeResponse reponse(Trajet trajet, String fenetreDebut, int position, boolean dejaAttribue) {
        Integer debut = minutes(fenetreDebut);
        String fenetreFin = debut == null ? fenetreDebut : format(debut + FENETRE_MINUTES);
        String gare = trajet.getVilleDepart() == null ? "la gare" : "la gare de " + trajet.getVilleDepart();

        String message = String.format(
                "Présente-toi à %s entre %s et %s. Tu es le %d%s sur ce créneau : en venant à cette heure, "
                        + "tu évites la file du départ de %s.",
                gare, fenetreDebut, fenetreFin, position, position == 1 ? "er" : "e", trajet.getHeureDepart());

        return new CreneauArriveeResponse(
                trajet.getVilleDepart(),
                trajet.getDate() == null ? null : trajet.getDate().toString(),
                trajet.getHeureDepart(),
                fenetreDebut,
                fenetreFin,
                position,
                message,
                dejaAttribue);
    }

    private int scoreA(AffluenceGareDto profil, int heure) {
        if (profil == null) {
            return 50;
        }
        String cle = String.format("%02d:00", heure);
        return profil.creneaux().stream()
                .filter(c -> c.heure().equals(cle))
                .findFirst()
                .map(c -> c.score())
                .orElse(50);
    }

    private static Integer minutes(String heure) {
        if (heure == null || heure.isBlank()) {
            return null;
        }
        try {
            String[] parts = heure.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return h * 60 + m;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String format(int minutes) {
        int m = Math.max(0, minutes);
        return String.format("%02d:%02d", (m / 60) % 24, m % 60);
    }
}
