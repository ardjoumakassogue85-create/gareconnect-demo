package com.hackathon.gares.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.gares.dto.CriteresRecherche;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiAssistantIaService implements AssistantIaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiAssistantIaService.class);
    private static final List<String> VILLES = List.of("Abidjan", "Bouake", "Yamoussoukro", "San-Pedro", "Korhogo");
    private static final Map<String, Integer> MOIS = Map.ofEntries(
            Map.entry("janvier", 1),
            Map.entry("fevrier", 2),
            Map.entry("mars", 3),
            Map.entry("avril", 4),
            Map.entry("mai", 5),
            Map.entry("juin", 6),
            Map.entry("juillet", 7),
            Map.entry("aout", 8),
            Map.entry("septembre", 9),
            Map.entry("octobre", 10),
            Map.entry("novembre", 11),
            Map.entry("decembre", 12)
    );

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiAssistantIaService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory())
                .build();
    }

    @Override
    public CriteresRecherche extraireCriteresRecherche(String texteLibre) {
        if (!hasText(texteLibre)) {
            return CriteresRecherche.vide();
        }

        CriteresRecherche criteresRapides = extraireCriteresRapides(texteLibre);

        if (!hasText(apiKey)) {
            LOGGER.info("Assistant IA: cle Gemini absente, utilisation de l'analyse locale.");
            return criteresRapides;
        }

        try {
            LOGGER.info("Assistant IA: appel Gemini en cours avec le modele {}.", model);
            String response = restClient.post()
                    .uri(baseUrl + "/models/" + model + ":generateContent")
                    .header("X-goog-api-key", apiKey)
                    .body(requestBody(texteLibre))
                    .retrieve()
                    .body(String.class);

            LOGGER.info("Assistant IA: reponse Gemini recue.");
            return fusionnerCriteres(parseResponse(response), criteresRapides);
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "Assistant IA: Gemini a refuse la requete avec le statut {}. Reponse: {}",
                    exception.getStatusCode(),
                    messageCourt(exception.getResponseBodyAsString())
            );
            return criteresRapides;
        } catch (Exception exception) {
            LOGGER.warn("Assistant IA: Gemini indisponible, utilisation de l'analyse locale. Cause: {}", exception.getMessage());
            return criteresRapides;
        }
    }

    private Map<String, Object> requestBody(String texteLibre) {
        return Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", prompt(texteLibre))
                        })
                },
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );
    }

    private String prompt(String texteLibre) {
        return """
                Tu extrais des criteres de recherche de trajet en Cote d'Ivoire a partir d'une phrase en francais.
                Villes possibles : Abidjan, Bouake, Yamoussoukro, San-Pedro, Korhogo.
                Convertis les dates relatives en date ISO reelle (yyyy-MM-dd) a partir de la date du jour %s.
                Convertis les dates ecrites en francais, par exemple "6 juillet 2026", en yyyy-MM-dd.
                Si la phrase demande le trajet le moins cher, le moindre cout ou pas cher, mets tri a "prix_asc".
                Si la phrase demande le trajet le plus cher, mets tri a "prix_desc".
                Si la phrase demande un seul trajet, un unique trajet, le meilleur trajet ou le premier resultat, mets nombreResultats a 1.
                Si une heure precise est demandee, mets heureDepart au format HH:mm.
                Si une compagnie est citee, mets son nom dans compagnie.
                Si un prix minimum est demande, mets prixMin. Si un tarif maximum est demande, mets budgetMax.
                Pour le statut, utilise uniquement "depart_imminent", "a_l_heure" ou "complet" quand c'est demande.
                Laisse un champ a null si absent du texte.
                Reponds uniquement en JSON.

                Phrase utilisateur : %s
                """.formatted(LocalDate.now(), texteLibre);
    }

    private CriteresRecherche parseResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || !hasText(textNode.asText())) {
            return CriteresRecherche.vide();
        }

        JsonNode criteres = objectMapper.readTree(textNode.asText());
        return new CriteresRecherche(
                textOrNull(criteres, "villeDepart"),
                textOrNull(criteres, "villeArrivee"),
                dateOrNull(criteres, "date"),
                integerOrNull(criteres, "budgetMax"),
                textOrNull(criteres, "tri"),
                integerOrNull(criteres, "nombreResultats"),
                textOrNull(criteres, "heureDepart"),
                textOrNull(criteres, "compagnie"),
                integerOrNull(criteres, "prixMin"),
                textOrNull(criteres, "statut")
        );
    }

    private CriteresRecherche extraireCriteresRapides(String texteLibre) {
        String texte = normaliser(texteLibre);
        String villeDepart = villeApres(texte, "depuis ", "de ", "d'");
        String villeArrivee = villeApres(texte, "vers ", "pour ", "a ");

        if (!hasText(villeDepart)) {
            villeDepart = premiereVilleDansTexte(texte, villeArrivee);
        }

        LocalDate date = dateRapide(texte);
        String tri = tri(texte);
        Integer nombreResultats = nombreResultats(texte);

        return new CriteresRecherche(
                villeDepart,
                villeArrivee,
                date,
                prixApres(texte, "moins de ", "maximum ", "max ", "budget ", "tarif max "),
                tri,
                nombreResultats,
                heureDepart(texte),
                compagnie(texte),
                prixApres(texte, "minimum ", "au moins "),
                statut(texte)
        );
    }

    private CriteresRecherche fusionnerCriteres(CriteresRecherche criteresIa, CriteresRecherche criteresRapides) {
        return new CriteresRecherche(
                hasText(criteresRapides.villeDepart()) ? criteresRapides.villeDepart() : criteresIa.villeDepart(),
                hasText(criteresRapides.villeArrivee()) ? criteresRapides.villeArrivee() : criteresIa.villeArrivee(),
                criteresRapides.date() != null ? criteresRapides.date() : criteresIa.date(),
                criteresRapides.budgetMax() != null ? criteresRapides.budgetMax() : criteresIa.budgetMax(),
                hasText(criteresRapides.tri()) ? criteresRapides.tri() : criteresIa.tri(),
                criteresRapides.nombreResultats() != null ? criteresRapides.nombreResultats() : criteresIa.nombreResultats(),
                hasText(criteresRapides.heureDepart()) ? criteresRapides.heureDepart() : criteresIa.heureDepart(),
                hasText(criteresRapides.compagnie()) ? criteresRapides.compagnie() : criteresIa.compagnie(),
                criteresRapides.prixMin() != null ? criteresRapides.prixMin() : criteresIa.prixMin(),
                hasText(criteresRapides.statut()) ? criteresRapides.statut() : criteresIa.statut()
        );
    }

    private LocalDate dateRapide(String texte) {
        if (texte.contains("aujourd'hui") || texte.contains("aujourdhui")) {
            return LocalDate.now();
        }
        if (texte.contains("demain")) {
            return LocalDate.now().plusDays(1);
        }

        Matcher dateTexte = Pattern.compile("(\\d{1,2})\\s+([a-z]+)\\s+(\\d{4})").matcher(texte);
        if (dateTexte.find()) {
            Integer mois = MOIS.get(dateTexte.group(2));
            if (mois != null) {
                return LocalDate.of(Integer.parseInt(dateTexte.group(3)), mois, Integer.parseInt(dateTexte.group(1)));
            }
        }

        Matcher dateNumerique = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})").matcher(texte);
        if (dateNumerique.find()) {
            return LocalDate.of(
                    Integer.parseInt(dateNumerique.group(3)),
                    Integer.parseInt(dateNumerique.group(2)),
                    Integer.parseInt(dateNumerique.group(1))
            );
        }

        return null;
    }

    private String tri(String texte) {
        if (contientUnDe(texte, "moins cher", "moindre cout", "pas cher", "economique")) {
            return "prix_asc";
        }
        if (contientUnDe(texte, "plus cher")) {
            return "prix_desc";
        }
        return null;
    }

    private Integer nombreResultats(String texte) {
        if (contientUnDe(texte, "un seul", "une seule", "unique", "premier resultat", "meilleur trajet")) {
            return 1;
        }

        Map<String, Integer> nombres = Map.of("deux", 2, "trois", 3, "quatre", 4, "cinq", 5);
        for (Map.Entry<String, Integer> entree : nombres.entrySet()) {
            if (texte.contains(entree.getKey() + " trajets") || texte.contains(entree.getKey() + " resultats")) {
                return entree.getValue();
            }
        }

        Matcher nombre = Pattern.compile("\\b([1-9])\\s+(?:trajets|resultats|resultat)\\b").matcher(texte);
        return nombre.find() ? Integer.parseInt(nombre.group(1)) : null;
    }

    private String heureDepart(String texte) {
        Matcher heure = Pattern.compile("\\b(\\d{1,2})(?:h|:)(\\d{2})?\\b").matcher(texte);
        if (!heure.find()) {
            return null;
        }

        int heures = Integer.parseInt(heure.group(1));
        String minutes = heure.group(2) == null ? "00" : heure.group(2);
        if (heures < 0 || heures > 23) {
            return null;
        }
        return "%02d:%s".formatted(heures, minutes);
    }

    private Integer prixApres(String texte, String... marqueurs) {
        for (String marqueur : marqueurs) {
            int index = texte.indexOf(normaliser(marqueur));
            if (index < 0) {
                continue;
            }
            Matcher montant = Pattern.compile("(\\d[\\d\\s.]*)").matcher(texte.substring(index));
            if (montant.find()) {
                return Integer.parseInt(montant.group(1).replaceAll("[\\s.]", ""));
            }
        }
        return null;
    }

    private String compagnie(String texte) {
        Matcher compagnie = Pattern.compile("\\b(?:compagnie|transporteur)\\s+([a-z0-9\\s-]+)").matcher(texte);
        return compagnie.find() ? compagnie.group(1).trim() : null;
    }

    private String statut(String texte) {
        if (texte.contains("complet")) {
            return "complet";
        }
        if (texte.contains("depart imminent")) {
            return "depart_imminent";
        }
        if (texte.contains("a l'heure")) {
            return "a_l_heure";
        }
        return null;
    }

    private String villeApres(String texte, String... marqueurs) {
        for (String marqueur : marqueurs) {
            int index = texte.indexOf(normaliser(marqueur));
            if (index < 0) {
                continue;
            }

            String reste = texte.substring(index + normaliser(marqueur).length()).trim();
            for (String ville : VILLES) {
                if (reste.startsWith(normaliser(ville))) {
                    return villeAffichee(ville);
                }
            }
        }
        return null;
    }

    private String premiereVilleDansTexte(String texte, String villeAExclure) {
        for (String ville : VILLES) {
            if (texte.contains(normaliser(ville)) && !villeAffichee(ville).equalsIgnoreCase(villeAExclure)) {
                return villeAffichee(ville);
            }
        }
        return null;
    }

    private boolean contientUnDe(String texte, String... valeurs) {
        for (String valeur : valeurs) {
            if (texte.contains(normaliser(valeur))) {
                return true;
            }
        }
        return false;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private LocalDate dateOrNull(JsonNode node, String field) {
        String value = textOrNull(node, field);
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer integerOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.canConvertToInt() ? value.asInt() : null;
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    private String messageCourt(String message) {
        if (message == null || message.isBlank()) {
            return "aucun detail";
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300) + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normaliser(String valeur) {
        return Normalizer.normalize(valeur, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String villeAffichee(String ville) {
        return switch (ville) {
            case "Bouake" -> "Bouaké";
            case "San-Pedro" -> "San-Pédro";
            default -> ville;
        };
    }
}
