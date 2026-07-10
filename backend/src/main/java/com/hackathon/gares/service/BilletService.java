package com.hackathon.gares.service;

import com.hackathon.gares.dto.BilletTokenResponse;
import com.hackathon.gares.dto.ClePubliqueResponse;
import com.hackathon.gares.dto.ValidationBilletResponse;
import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.Reservation;
import com.hackathon.gares.model.StatutReservation;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.CompagnieProfileRepository;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

/**
 * Billet infalsifiable.
 *
 * Le QR code du billet ne contient pas un simple identifiant, mais un JETON SIGNE
 * (JWT RS256) : les donnees du billet + une signature RSA faite avec la cle PRIVEE
 * du serveur. Toute modification invalide la signature ; fabriquer un faux billet
 * est impossible sans la cle privee.
 *
 * - Verification EN LIGNE : le serveur verifie la signature ET consulte la base
 *   pour bloquer la reutilisation (un billet ne se valide qu'une fois).
 * - Verification HORS-LIGNE : la cle PUBLIQUE (sans danger a distribuer) permet a
 *   l'app controleur de verifier l'authenticite sans reseau ; le double-usage est
 *   reconcilie a la reconnexion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BilletService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CompagnieProfileRepository compagnieProfileRepository;

    /** Cle privee (PKCS8 base64) pour signer les billets ; a definir en production. */
    @Value("${app.billet.private-key:}")
    private String clePriveeBase64;

    private KeyPair keyPair;

    @PostConstruct
    void init() {
        if (clePriveeBase64 != null && !clePriveeBase64.isBlank()) {
            try {
                KeyFactory fabrique = KeyFactory.getInstance("RSA");
                PrivateKey privee = fabrique.generatePrivate(
                        new PKCS8EncodedKeySpec(Base64.getDecoder().decode(clePriveeBase64.trim())));
                RSAPrivateCrtKey crt = (RSAPrivateCrtKey) privee;
                PublicKey publique = fabrique.generatePublic(
                        new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
                this.keyPair = new KeyPair(publique, privee);
                log.info("Cle de signature des billets chargee depuis la configuration.");
                return;
            } catch (Exception exception) {
                log.warn("Cle billet configuree illisible ({}) : generation d'une cle ephemere.",
                        exception.getMessage());
            }
        }
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(2048);
            this.keyPair = generateur.generateKeyPair();
            log.warn("Cle de signature des billets EPHEMERE (regeneree a chaque demarrage). "
                    + "Definis app.billet.private-key (BILLET_PRIVATE_KEY) en production.");
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de preparer la cle de signature des billets", exception);
        }
    }

    /** Jeton signe pour le billet du client connecte (encode dans son QR code). */
    @Transactional(readOnly = true)
    public BilletTokenResponse genererTokenBillet(Authentication authentication, Long reservationId) {
        User client = getUser(authentication);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable"));
        if (reservation.getClient() == null || !reservation.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reservation inaccessible");
        }
        return new BilletTokenResponse(signer(reservation), reservation.getCodeBillet());
    }

    private String signer(Reservation reservation) {
        return Jwts.builder()
                .subject(String.valueOf(reservation.getId()))
                .claim("code", reservation.getCodeBillet())
                .claim("dep", reservation.getVilleDepart())
                .claim("arr", reservation.getVilleArrivee())
                .claim("date", reservation.getDate() == null ? null : reservation.getDate().toString())
                .claim("heure", reservation.getHeure())
                .claim("comp", reservation.getCompagnie())
                .claim("pax", reservation.getClient() == null ? null : reservation.getClient().getNom())
                .claim("places", reservation.getNombreTickets())
                .issuedAt(new Date())
                .expiration(dateExpiration(reservation))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    /** Le billet expire a la fin du lendemain du voyage (marge d'embarquement / fuseau). */
    private Date dateExpiration(Reservation reservation) {
        LocalDate base = reservation.getDate() != null ? reservation.getDate() : LocalDate.now();
        Instant expiration = base.plusDays(1).atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(expiration);
    }

    /** Cle publique (SPKI base64) a mettre en cache sur l'app controleur pour le hors-ligne. */
    public ClePubliqueResponse clePublique() {
        return new ClePubliqueResponse(
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                "RS256");
    }

    /** Controle en ligne : authenticite (signature) + anti-reutilisation (base). */
    @Transactional
    public ValidationBilletResponse valider(Authentication agent, String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(keyPair.getPublic()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException exception) {
            return rejet("Billet expiré");
        } catch (Exception exception) {
            return rejet("Signature invalide — billet falsifié ou illisible");
        }

        long billetId;
        try {
            billetId = Long.parseLong(claims.getSubject());
        } catch (Exception exception) {
            return rejet("Billet illisible");
        }

        Reservation reservation = reservationRepository.findById(billetId).orElse(null);
        if (reservation == null) {
            return rejet("Billet inconnu en base");
        }
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            return rejet("Billet annulé");
        }

        // L'agent ne peut valider que les billets de SA compagnie.
        String compagnieAgent = compagnieDeLAgent(agent);
        if (compagnieAgent != null && reservation.getCompagnie() != null
                && !reservation.getCompagnie().equalsIgnoreCase(compagnieAgent)) {
            return rejet("Billet d'une autre compagnie — validation refusée");
        }

        boolean dejaUtilise = reservation.getValideLe() != null;
        if (!dejaUtilise) {
            reservation.setValideLe(Instant.now());
            reservation.setValideePar(agent == null ? "agent" : agent.getName());
            reservationRepository.save(reservation);
        }

        return new ValidationBilletResponse(
                !dejaUtilise,
                dejaUtilise ? "Billet DÉJÀ utilisé (embarquement refusé)" : "Billet valide — embarquement autorisé",
                reservation.getClient() == null ? null : reservation.getClient().getNom(),
                reservation.getVilleDepart() + " → " + reservation.getVilleArrivee(),
                reservation.getDate() == null ? null : reservation.getDate().toString(),
                reservation.getHeure(),
                reservation.getCompagnie(),
                reservation.getNombreTickets(),
                dejaUtilise,
                reservation.getValideLe() == null ? null : reservation.getValideLe().toString());
    }

    private ValidationBilletResponse rejet(String message) {
        return new ValidationBilletResponse(false, message, null, null, null, null, null, 0, false, null);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private String compagnieDeLAgent(Authentication agent) {
        if (agent == null) {
            return null;
        }
        return userRepository.findByEmail(agent.getName())
                .flatMap(compagnieProfileRepository::findByUser)
                .map(CompagnieProfile::getNom)
                .orElse(null);
    }
}
