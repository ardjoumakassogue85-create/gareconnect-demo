package com.hackathon.gares.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hackathon.gares.dto.ContexteAffluenceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enrichissement contextuel de l'affluence par l'IA.
 *
 * Un modele numerique ne "sait" pas qu'un jour est ferie, qu'il y a un match ou
 * un grand marche. Gemini, si. Pour une gare et une date, on lui demande s'il
 * existe un evenement qui gonfle ou reduit l'affluence, et on renvoie un facteur
 * multiplicatif + une explication. Resultat mis en cache (par ville+date) pour ne
 * pas rappeler l'IA a chaque requete, et repli neutre si l'IA est indisponible.
 */
@Service
@RequiredArgsConstructor
public class ContexteAffluenceService {

    private final GeminiClient geminiClient;
    private final Map<String, ContexteAffluenceDto> cache = new ConcurrentHashMap<>();

    private static final String[] JOURS_FR = {
            "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"
    };

    public ContexteAffluenceDto pour(String ville, LocalDate date) {
        if (ville == null || ville.isBlank()) {
            return neutre();
        }
        LocalDate jour = date == null ? LocalDate.now() : date;
        String cle = ville.trim().toLowerCase() + "|" + jour;
        return cache.computeIfAbsent(cle, k -> calculer(ville.trim(), jour));
    }

    private ContexteAffluenceDto calculer(String ville, LocalDate date) {
        if (!geminiClient.estActif()) {
            return neutre();
        }

        Optional<JsonNode> reponse = geminiClient.genererJson(prompt(ville, date));
        if (reponse.isEmpty()) {
            return neutre();
        }

        JsonNode json = reponse.get();
        double facteur = json.path("facteur").isNumber() ? json.path("facteur").asDouble(1.0) : 1.0;
        facteur = Math.max(0.5, Math.min(2.0, facteur));
        facteur = Math.round(facteur * 100.0) / 100.0;

        String raison = texte(json, "raison");
        boolean actif = raison != null && Math.abs(facteur - 1.0) >= 0.1;
        return new ContexteAffluenceDto(facteur, actif ? raison : null, actif);
    }

    private String prompt(String ville, LocalDate date) {
        String jourFr = JOURS_FR[date.getDayOfWeek().getValue() - 1];
        return """
                En Cote d'Ivoire, le %s (%s) a %s, y a-t-il un jour ferie, une fete, un grand jour de marche,
                ou un evenement notable (match, concert, rentree scolaire, veille de fete) qui augmente ou
                diminue nettement l'affluence dans les gares routieres ?
                Reponds UNIQUEMENT en JSON :
                {"facteur": <nombre entre 0.5 et 2.0, 1.0 s'il n'y a rien de notable>, "raison": "<courte phrase en francais expliquant, ou null si rien de notable>"}
                """.formatted(date, jourFr, ville);
    }

    private ContexteAffluenceDto neutre() {
        return new ContexteAffluenceDto(1.0, null, false);
    }

    private static String texte(JsonNode node, String champ) {
        JsonNode v = node.path(champ);
        return v.isMissingNode() || v.isNull() || v.asText("").isBlank() || "null".equalsIgnoreCase(v.asText())
                ? null
                : v.asText().trim();
    }
}
