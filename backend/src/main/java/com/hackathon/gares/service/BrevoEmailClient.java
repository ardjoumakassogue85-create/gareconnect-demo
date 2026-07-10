package com.hackathon.gares.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envoi d'emails via l'API HTTP de Brevo (https://api.brevo.com/v3/smtp/email).
 *
 * Contrairement au SMTP (port 587/465, bloque par beaucoup d'hebergeurs cloud
 * comme Railway), cette API passe par HTTPS/443, jamais filtre. On l'utilise en
 * ligne ; en local on peut garder le SMTP Gmail classique.
 *
 * Actif seulement si BREVO_API_KEY est defini.
 */
@Component
@Slf4j
public class BrevoEmailClient {

    private final String apiKey;
    private final RestClient restClient;

    public BrevoEmailClient(@Value("${brevo.api-key:}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(6));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Vrai si une cle API Brevo est configuree. */
    public boolean estActif() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Envoie un email texte via l'API Brevo. Propage une exception en cas d'echec
     * (l'appelant decide quoi en faire).
     */
    public void envoyer(String fromEmail, String fromNom, String toEmail, String sujet, String texte) {
        restClient.post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("email", fromEmail, "name", fromNom),
                        "to", List.of(Map.of("email", toEmail)),
                        "subject", sujet,
                        "textContent", texte
                ))
                .retrieve()
                .toBodilessEntity();
        log.info("Email Brevo envoye a {}", toEmail);
    }
}
