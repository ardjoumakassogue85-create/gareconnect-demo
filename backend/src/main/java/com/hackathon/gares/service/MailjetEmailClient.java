package com.hackathon.gares.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Envoi d'emails via l'API HTTP de Mailjet (https://api.mailjet.com/v3.1/send).
 *
 * Contrairement au SMTP (port 587, bloque par Railway/Render et beaucoup d'hebergeurs
 * cloud), cette API passe par HTTPS/443, jamais filtre. On l'utilise en ligne ; en
 * local on garde le SMTP Gmail classique (repli automatique).
 *
 * Actif seulement si MAILJET_API_KEY et MAILJET_SECRET_KEY sont definis.
 */
@Component
@Slf4j
public class MailjetEmailClient {

    private final String apiKey;
    private final String secretKey;
    private final RestClient restClient;

    public MailjetEmailClient(
            @Value("${mailjet.api-key:}") String apiKey,
            @Value("${mailjet.secret-key:}") String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(6));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Vrai si les deux cles Mailjet sont configurees. */
    public boolean estActif() {
        return apiKey != null && !apiKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    /**
     * Envoie un email texte via l'API Mailjet (auth HTTP Basic cle:secret).
     * Propage une exception en cas d'echec (l'appelant decide quoi en faire).
     */
    public void envoyer(String fromEmail, String fromNom, String toEmail, String sujet, String texte) {
        String auth = Base64.getEncoder().encodeToString(
                (apiKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));

        restClient.post()
                .uri("https://api.mailjet.com/v3.1/send")
                .header("Authorization", "Basic " + auth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "Messages", List.of(Map.of(
                                "From", Map.of("Email", fromEmail, "Name", fromNom),
                                "To", List.of(Map.of("Email", toEmail)),
                                "Subject", sujet,
                                "TextPart", texte
                        ))
                ))
                .retrieve()
                .toBodilessEntity();
        log.info("Email Mailjet envoye a {}", toEmail);
    }
}
