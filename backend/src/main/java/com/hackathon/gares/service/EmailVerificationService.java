package com.hackathon.gares.service;

import com.hackathon.gares.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final BrevoEmailClient brevoEmailClient;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.email.from:}")
    private String mailFrom;

    public void envoyerVerification(User user) {
        String texte = """
                Bonjour %s,

                Voici ton code de verification ResaGares :

                %s

                Ce code expire dans 15 minutes.
                Si tu n'es pas a l'origine de cette inscription, ignore ce message.
                """.formatted(user.getNom(), user.getEmailVerificationCode());

        envoyer(user.getEmail(), "Code de verification ResaGares", texte);
        log.info("Email de verification envoye a {}", user.getEmail());
    }

    public void envoyerReinitialisation(User user, String lienReinitialisation) {
        String texte = """
                Bonjour %s,

                Tu as demande a reinitialiser ton mot de passe ResaGares.
                Clique sur le lien ci-dessous pour en choisir un nouveau :

                %s

                Ce lien expire dans 30 minutes.
                Si tu n'es pas a l'origine de cette demande, ignore ce message :
                ton mot de passe actuel reste inchange.
                """.formatted(user.getNom(), lienReinitialisation);

        envoyer(user.getEmail(), "Reinitialisation de ton mot de passe ResaGares", texte);
        log.info("Email de reinitialisation envoye a {}", user.getEmail());
    }

    /**
     * Envoi unifie : priorite a l'API Brevo (HTTPS, marche en cloud) si une cle
     * est configuree, sinon repli sur le SMTP classique (dev local avec Gmail).
     */
    private void envoyer(String destinataire, String sujet, String texte) {
        if (brevoEmailClient.estActif()) {
            brevoEmailClient.envoyer(adresseExpediteur(), "GareConnect", destinataire, sujet, texte);
            return;
        }

        if (mailHost == null || mailHost.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Envoi email non configure. Ajoute BREVO_API_KEY ou les parametres SMTP."
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(adresseExpediteur());
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(texte);
        mailSender.send(message);
    }

    private String adresseExpediteur() {
        if (mailFrom != null && !mailFrom.isBlank()) {
            return mailFrom;
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername;
        }
        return "no-reply@resagares.local";
    }
}
