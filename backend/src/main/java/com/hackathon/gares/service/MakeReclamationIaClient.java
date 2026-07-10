package com.hackathon.gares.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.gares.dto.ReclamationIaRequest;
import com.hackathon.gares.dto.ReclamationIaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * Appelle le scenario Make.com charge de traiter automatiquement les
 * reclamations clients.
 *
 * En cas d'absence d'URL configuree, d'erreur ou de timeout, on bascule
 * par defaut vers une escalade humaine (transmission a la compagnie / gare
 * concernee) plutot que de bloquer ou de renvoyer une reponse hasardeuse.
 */
@Service
public class MakeReclamationIaClient implements ReclamationIaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MakeReclamationIaClient.class);

    private final String webhookUrl;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MakeReclamationIaClient(
            @Value("${make.reclamation-webhook-url:}") String webhookUrl,
            ObjectMapper objectMapper
    ) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory())
                .build();
    }

    @Override
    public ReclamationIaResponse traiter(ReclamationIaRequest request) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            LOGGER.info("Reclamation IA: URL du webhook Make.com absente, escalade vers un agent.");
            return ReclamationIaResponse.escalade("Webhook non configure");
        }

        try {
            LOGGER.info("Reclamation IA: appel du webhook Make.com pour la reclamation {}.", request.reclamationId());
            String body = restClient.post()
                    .uri(webhookUrl)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            return parseReponse(body);
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "Reclamation IA: le webhook Make.com a repondu avec le statut {}. Escalade vers un agent.",
                    exception.getStatusCode()
            );
            return ReclamationIaResponse.escalade("Webhook en erreur (" + exception.getStatusCode() + ")");
        } catch (Exception exception) {
            LOGGER.warn(
                    "Reclamation IA: webhook Make.com indisponible ({}). Escalade vers un agent.",
                    exception.getMessage()
            );
            return ReclamationIaResponse.escalade("Webhook indisponible");
        }
    }

    private ReclamationIaResponse parseReponse(String body) {
        if (body == null || body.isBlank()) {
            return ReclamationIaResponse.escalade("Reponse vide du webhook");
        }

        try {
            JsonNode root = objectMapper.readTree(body);

            // Cas d'un scenario Make qui renvoie un texte brut (pas de JSON).
            if (root.isTextual()) {
                String texte = root.asText();
                return texte.isBlank()
                        ? ReclamationIaResponse.escalade("Reponse vide du webhook")
                        : new ReclamationIaResponse(true, texte.trim(), null);
            }

            Boolean peutRepondre = root.has("peutRepondre") && !root.path("peutRepondre").isNull()
                    ? root.path("peutRepondre").asBoolean()
                    : null;
            String reponse = textOrNull(root, "reponse");
            String raison = textOrNull(root, "raison");

            return new ReclamationIaResponse(peutRepondre, reponse, raison);
        } catch (Exception exception) {
            // Le corps n'est pas du JSON : on le traite comme une reponse texte brute.
            String texte = body.trim();
            if (texte.isEmpty()) {
                return ReclamationIaResponse.escalade("Reponse illisible du webhook");
            }
            return new ReclamationIaResponse(true, texte, null);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }
}
