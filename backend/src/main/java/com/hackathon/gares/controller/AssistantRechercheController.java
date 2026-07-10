package com.hackathon.gares.controller;

import com.hackathon.gares.dto.AssistantRechercheRequest;
import com.hackathon.gares.dto.AssistantRechercheResponse;
import com.hackathon.gares.dto.CriteresRecherche;
import com.hackathon.gares.dto.TrajetDto;
import com.hackathon.gares.service.AssistantIaService;
import com.hackathon.gares.service.VoyageurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/trajets")
@RequiredArgsConstructor
public class AssistantRechercheController {

    private final AssistantIaService assistantIaService;
    private final VoyageurService voyageurService;

    @PostMapping("/recherche-ia")
    public AssistantRechercheResponse rechercherAvecIa(@Valid @RequestBody AssistantRechercheRequest request) {
        CriteresRecherche criteres = appliquerContexte(
                assistantIaService.extraireCriteresRecherche(request.texteLibre()),
                request
        );
        List<TrajetDto> trajetsBase = voyageurService.rechercher(
                criteres.villeDepart(),
                criteres.villeArrivee(),
                criteres.date() == null ? null : criteres.date().toString()
        );
        List<TrajetDto> resultats = appliquerFiltresStricts(trajetsBase, criteres);

        if (!resultats.isEmpty()) {
            return new AssistantRechercheResponse(criteres, resultats, null, false);
        }

        List<TrajetDto> suggestions = suggestionsProches(candidatsSuggestion(criteres, trajetsBase), criteres);
        if (!suggestions.isEmpty()) {
            return new AssistantRechercheResponse(
                    criteres,
                    suggestions,
                    "Je suis desole, je n'ai pas trouve de trajet avec toutes ces specifications. Voici la proposition la plus proche de ta demande.",
                    true
            );
        }

        return new AssistantRechercheResponse(
                criteres,
                List.of(),
                "Je suis desole, je n'ai pas trouve de trajet correspondant a cette recherche.",
                false
        );
    }

    private CriteresRecherche appliquerContexte(CriteresRecherche criteres, AssistantRechercheRequest request) {
        return new CriteresRecherche(
                texteOuDefaut(criteres.villeDepart(), request.villeDepart()),
                texteOuDefaut(criteres.villeArrivee(), request.villeArrivee()),
                criteres.date() != null ? criteres.date() : parseDateOuNull(request.date()),
                criteres.budgetMax(),
                criteres.tri(),
                criteres.nombreResultats(),
                criteres.heureDepart(),
                criteres.compagnie(),
                criteres.prixMin(),
                criteres.statut()
        );
    }

    private Comparator<TrajetDto> comparateur(CriteresRecherche criteres) {
        Comparator<TrajetDto> prix = Comparator.comparingInt(TrajetDto::prix);
        if ("prix_desc".equalsIgnoreCase(criteres.tri())) {
            prix = prix.reversed();
        }

        return Comparator
                .comparing(TrajetDto::villeArrivee, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TrajetDto::date, Comparator.nullsLast(String::compareTo))
                .thenComparing(prix)
                .thenComparing(TrajetDto::heureDepart, Comparator.nullsLast(String::compareTo))
                .thenComparingInt(this::prioriteStatut)
                .thenComparing(TrajetDto::compagnie, String.CASE_INSENSITIVE_ORDER);
    }

    private List<TrajetDto> appliquerFiltresStricts(List<TrajetDto> trajets, CriteresRecherche criteres) {
        return trajets.stream()
                .filter(trajet -> criteres.heureDepart() == null || trajet.heureDepart().equals(criteres.heureDepart()))
                .filter(trajet -> criteres.compagnie() == null || contient(trajet.compagnie(), criteres.compagnie()))
                .filter(trajet -> criteres.prixMin() == null || trajet.prix() >= criteres.prixMin())
                .filter(trajet -> criteres.budgetMax() == null || trajet.prix() <= criteres.budgetMax())
                .filter(trajet -> correspondAuStatut(trajet, criteres.statut()))
                .sorted(comparateur(criteres))
                .limit(limite(criteres))
                .toList();
    }

    private List<TrajetDto> suggestionsProches(List<TrajetDto> trajets, CriteresRecherche criteres) {
        List<TrajetDto> tries = trajets.stream()
                .sorted(comparateurProximite(criteres))
                .toList();

        return voisinsDirects(tries, criteres).stream()
                .limit(limiteSuggestion(criteres))
                .toList();
    }

    private List<TrajetDto> voisinsDirects(List<TrajetDto> trajets, CriteresRecherche criteres) {
        if (trajets.isEmpty()) {
            return trajets;
        }

        List<TrajetDto> voisins = trajets;
        if (criteres.date() != null) {
            long meilleurEcart = voisins.stream().mapToLong(trajet -> ecartJours(trajet, criteres)).min().orElse(0);
            voisins = voisins.stream().filter(trajet -> ecartJours(trajet, criteres) == meilleurEcart).toList();
        }

        if (criterePrixPresent(criteres)) {
            long meilleurEcart = voisins.stream().mapToLong(trajet -> ecartPrix(trajet, criteres)).min().orElse(0);
            voisins = voisins.stream().filter(trajet -> ecartPrix(trajet, criteres) == meilleurEcart).toList();
        }

        if (criteres.heureDepart() != null && !criteres.heureDepart().isBlank()) {
            long meilleurEcart = voisins.stream().mapToLong(trajet -> ecartMinutes(trajet, criteres.heureDepart())).min().orElse(0);
            voisins = voisins.stream().filter(trajet -> ecartMinutes(trajet, criteres.heureDepart()) == meilleurEcart).toList();
        }

        if (criteres.statut() != null && !criteres.statut().isBlank()) {
            int meilleurEcart = voisins.stream().mapToInt(trajet -> ecartStatut(trajet, criteres.statut())).min().orElse(0);
            voisins = voisins.stream().filter(trajet -> ecartStatut(trajet, criteres.statut()) == meilleurEcart).toList();
        }

        if (criteres.compagnie() != null && !criteres.compagnie().isBlank()) {
            int meilleurEcart = voisins.stream().mapToInt(trajet -> ecartCompagnie(trajet, criteres.compagnie())).min().orElse(0);
            voisins = voisins.stream().filter(trajet -> ecartCompagnie(trajet, criteres.compagnie()) == meilleurEcart).toList();
        }

        return voisins.isEmpty() ? List.of(trajets.get(0)) : voisins;
    }

    private Comparator<TrajetDto> comparateurProximite(CriteresRecherche criteres) {
        return Comparator
                .comparingInt((TrajetDto trajet) -> scoreProximite(trajet, criteres))
                .thenComparingLong(trajet -> ecartJours(trajet, criteres))
                .thenComparingLong(trajet -> ecartPrix(trajet, criteres))
                .thenComparingLong(trajet -> ecartMinutes(trajet, criteres.heureDepart()))
                .thenComparingInt(trajet -> ecartStatut(trajet, criteres.statut()))
                .thenComparingInt(trajet -> ecartCompagnie(trajet, criteres.compagnie()))
                .thenComparing(comparateur(criteres));
    }

    private List<TrajetDto> candidatsSuggestion(CriteresRecherche criteres, List<TrajetDto> trajetsBase) {
        Map<String, TrajetDto> candidats = new LinkedHashMap<>();
        ajouter(candidats, trajetsBase);
        ajouter(candidats, voyageurService.rechercher(criteres.villeDepart(), criteres.villeArrivee(), null));
        return List.copyOf(candidats.values());
    }

    private void ajouter(Map<String, TrajetDto> candidats, List<TrajetDto> trajets) {
        for (TrajetDto trajet : trajets) {
            candidats.putIfAbsent(trajet.id(), trajet);
        }
    }

    private int scoreProximite(TrajetDto trajet, CriteresRecherche criteres) {
        int score = 0;
        if (criteres.villeArrivee() != null && !criteres.villeArrivee().isBlank()
                && !trajet.villeArrivee().equalsIgnoreCase(criteres.villeArrivee())) {
            score += 1000;
        }
        if (criteres.villeDepart() != null && !criteres.villeDepart().isBlank()
                && !trajet.villeDepart().equalsIgnoreCase(criteres.villeDepart())) {
            score += 350;
        }
        if (criteres.budgetMax() != null && trajet.prix() > criteres.budgetMax()) {
            score += Math.min(250, (trajet.prix() - criteres.budgetMax()) / 100);
        }
        if (criteres.prixMin() != null && trajet.prix() < criteres.prixMin()) {
            score += Math.min(250, (criteres.prixMin() - trajet.prix()) / 100);
        }
        return score;
    }

    private int prioriteStatut(TrajetDto trajet) {
        if (trajet.placesDisponibles() <= 0) {
            return 2;
        }
        return trajet.heureDepart().compareTo("07:30") <= 0 ? 0 : 1;
    }

    private long limite(CriteresRecherche criteres) {
        if (criteres.nombreResultats() == null || criteres.nombreResultats() < 1) {
            return Long.MAX_VALUE;
        }
        return Math.min(criteres.nombreResultats(), 20);
    }

    private long limiteSuggestion(CriteresRecherche criteres) {
        return 3;
    }

    private boolean criterePrixPresent(CriteresRecherche criteres) {
        return criteres.budgetMax() != null
                || criteres.prixMin() != null
                || "prix_asc".equalsIgnoreCase(criteres.tri())
                || "prix_desc".equalsIgnoreCase(criteres.tri());
    }

    private boolean correspondAuStatut(TrajetDto trajet, String statut) {
        if (statut == null || statut.isBlank()) {
            return true;
        }

        return switch (statut.toLowerCase(Locale.ROOT)) {
            case "complet" -> trajet.placesDisponibles() <= 0;
            case "depart_imminent" -> trajet.placesDisponibles() > 0 && trajet.heureDepart().compareTo("07:30") <= 0;
            case "a_l_heure" -> trajet.placesDisponibles() > 0 && trajet.heureDepart().compareTo("07:30") > 0;
            default -> true;
        };
    }

    private boolean contient(String valeur, String filtre) {
        return valeur != null && filtre != null
                && valeur.toLowerCase(Locale.ROOT).contains(filtre.toLowerCase(Locale.ROOT));
    }

    private String texteOuDefaut(String valeur, String defaut) {
        return valeur == null || valeur.isBlank() ? defaut : valeur;
    }

    private java.time.LocalDate parseDateOuNull(String date) {
        try {
            return date == null || date.isBlank() ? null : java.time.LocalDate.parse(date);
        } catch (Exception ignored) {
            return null;
        }
    }

    private long ecartMinutes(TrajetDto trajet, String heureDemandee) {
        if (heureDemandee == null || heureDemandee.isBlank()) {
            return 0;
        }
        try {
            return Math.abs(ChronoUnit.MINUTES.between(LocalTime.parse(heureDemandee), LocalTime.parse(trajet.heureDepart())));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long ecartPrix(TrajetDto trajet, CriteresRecherche criteres) {
        if (criteres.budgetMax() != null) {
            return Math.abs((long) trajet.prix() - criteres.budgetMax());
        }
        if (criteres.prixMin() != null) {
            return Math.abs((long) trajet.prix() - criteres.prixMin());
        }
        if ("prix_desc".equalsIgnoreCase(criteres.tri())) {
            return -trajet.prix();
        }
        return trajet.prix();
    }

    private int ecartStatut(TrajetDto trajet, String statut) {
        return correspondAuStatut(trajet, statut) ? 0 : 1;
    }

    private int ecartCompagnie(TrajetDto trajet, String compagnie) {
        return compagnie == null || compagnie.isBlank() || contient(trajet.compagnie(), compagnie) ? 0 : 1;
    }

    private long ecartJours(TrajetDto trajet, CriteresRecherche criteres) {
        if (criteres.date() == null || trajet.date() == null || trajet.date().isBlank()) {
            return 0;
        }
        try {
            return Math.abs(ChronoUnit.DAYS.between(criteres.date(), java.time.LocalDate.parse(trajet.date())));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
