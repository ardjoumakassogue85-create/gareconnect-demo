package com.hackathon.gares.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Client Gemini reutilisable et defensif.
 *
 * Toutes les fonctionnalites IA (assistant anti-file, enrichissement contextuel)
 * passent par ici. La regle d'or : l'IA ne doit jamais casser l'application.
 * En cas de cle absente, de timeout ou d'erreur, on renvoie Optional.empty() et
 * l'appelant retombe sur sa logique deterministe.
 */
@Component
@Slf4j
public class GeminiClient {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-flash-latest}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(6));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Vrai si une cle Gemini est configuree. */
    public boolean estActif() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Envoie un prompt a Gemini en forcant une reponse JSON.
     * Renvoie le noeud JSON produit par le modele, ou Optional.empty() en cas
     * d'indisponibilite / erreur (jamais d'exception propagee).
     */
    public Optional<JsonNode> genererJson(String prompt) {
        if (!estActif()) {
            return Optional.empty();
        }
        try {
            String response = restClient.post()
                    .uri(baseUrl + "/models/" + model + ":generateContent")
                    .header("X-goog-api-key", apiKey)
                    .body(Map.of(
                            "contents", new Object[]{
                                    Map.of("parts", new Object[]{Map.of("text", prompt)})
                            },
                            "generationConfig", Map.of("responseMimeType", "application/json")
                    ))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode texte = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (texte.isMissingNode() || texte.asText("").isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(texte.asText()));
        } catch (Exception exception) {
            log.warn("Gemini indisponible ({}). Repli sur la logique deterministe.", exception.getMessage());
            return Optional.empty();
        }
    }
}
