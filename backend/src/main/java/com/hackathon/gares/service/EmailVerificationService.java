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

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.email.from:}")
    private String mailFrom;

    public void envoyerVerification(User user) {
        if (mailHost == null || mailHost.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Envoi email non configure. Ajoute les parametres SMTP dans le fichier .env."
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(adresseExpediteur());
        message.setTo(user.getEmail());
        message.setSubject("Code de verification ResaGares");
        message.setText("""
                Bonjour %s,

                Voici ton code de verification ResaGares :

                %s

                Ce code expire dans 15 minutes.
                Si tu n'es pas a l'origine de cette inscription, ignore ce message.
                """.formatted(user.getNom(), user.getEmailVerificationCode()));

        mailSender.send(message);
        log.info("Email de verification envoye a {}", user.getEmail());
    }

    public void envoyerReinitialisation(User user, String lienReinitialisation) {
        if (mailHost == null || mailHost.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Envoi email non configure. Ajoute les parametres SMTP dans le fichier .env."
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(adresseExpediteur());
        message.setTo(user.getEmail());
        message.setSubject("Reinitialisation de ton mot de passe ResaGares");
        message.setText("""
                Bonjour %s,

                Tu as demande a reinitialiser ton mot de passe ResaGares.
                Clique sur le lien ci-dessous pour en choisir un nouveau :

                %s

                Ce lien expire dans 30 minutes.
                Si tu n'es pas a l'origine de cette demande, ignore ce message :
                ton mot de passe actuel reste inchange.
                """.formatted(user.getNom(), lienReinitialisation));

        mailSender.send(message);
        log.info("Email de reinitialisation envoye a {}", user.getEmail());
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
