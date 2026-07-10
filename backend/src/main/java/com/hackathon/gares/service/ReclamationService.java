package com.hackathon.gares.service;

import com.hackathon.gares.dto.ReclamationDto;
import com.hackathon.gares.dto.ReclamationIaRequest;
import com.hackathon.gares.dto.ReclamationIaResponse;
import com.hackathon.gares.dto.ReclamationStatutRequest;
import com.hackathon.gares.model.*;
import com.hackathon.gares.repository.CompagnieProfileRepository;
import com.hackathon.gares.repository.ReclamationRepository;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReclamationService {

    private static final String MESSAGE_ESCALADE =
            "Votre demande necessite l'intervention d'un agent. Elle a ete transmise a la gare concernee, "
                    + "qui reviendra vers vous rapidement.";

    private final ReclamationRepository reclamationRepository;
    private final UserRepository userRepository;
    private final CompagnieProfileRepository compagnieProfileRepository;
    private final ReservationRepository reservationRepository;
    private final MetierMapper mapper;
    private final ReclamationIaClient reclamationIaClient;

    @Transactional(readOnly = true)
    public List<ReclamationDto> listerMesReclamations(Authentication authentication) {
        return reclamationRepository.findByClientOrderByMajLeDesc(getUser(authentication)).stream()
                .map(mapper::toReclamationDto)
                .toList();
    }

    @Transactional
    public ReclamationDto demarrer(Authentication authentication, String message) {
        User client = getUser(authentication);
        Instant maintenant = Instant.now();
        Reclamation reclamation = Reclamation.builder()
                .client(client)
                .compagnie(resoudreCompagnie(client, message))
                .sujet(sujetDepuis(message))
                .statut(StatutReclamation.EN_ATTENTE_ADMIN)
                .creeLe(maintenant)
                .majLe(maintenant)
                .build();
        reclamation.getMessages().add(message(reclamation, AuteurMessage.CLIENT, message, maintenant));

        traiterAvecIa(reclamation, message, maintenant.plusMillis(1));

        return mapper.toReclamationDto(reclamationRepository.save(reclamation));
    }

    @Transactional
    public ReclamationDto ajouterMessage(Authentication authentication, Long id, String texte) {
        Reclamation reclamation = reclamationDuClient(authentication, id);
        Instant maintenant = Instant.now();
        reclamation.getMessages().add(message(reclamation, AuteurMessage.CLIENT, texte, maintenant));

        traiterAvecIa(reclamation, texte, maintenant.plusMillis(1));

        return mapper.toReclamationDto(reclamation);
    }

    @Transactional(readOnly = true)
    public List<ReclamationDto> listerPourCompagnie(CompagnieProfile compagnieConnectee) {
        return reclamationRepository
                .findByStatutAndCompagnieOrderByMajLeDesc(StatutReclamation.EN_ATTENTE_ADMIN, compagnieConnectee)
                .stream()
                .map(mapper::toReclamationDto)
                .toList();
    }

    @Transactional
    public ReclamationDto repondre(CompagnieProfile compagnieConnectee, Long id, ReclamationStatutRequest request) {
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reclamation introuvable"));
        if (reclamation.getCompagnie() == null
                || !reclamation.getCompagnie().getId().equals(compagnieConnectee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette reclamation appartient a une autre compagnie");
        }
        Instant maintenant = Instant.now();
        if (request.reponse() != null && !request.reponse().isBlank()) {
            reclamation.getMessages().add(message(reclamation, AuteurMessage.ADMIN, request.reponse(), maintenant));
        }
        if (request.statut() != null && !request.statut().isBlank()) {
            reclamation.setStatut(StatutReclamation.valueOf(request.statut()));
        } else {
            reclamation.setStatut(StatutReclamation.RESOLUE_ADMIN);
        }
        reclamation.setMajLe(maintenant);
        return mapper.toReclamationDto(reclamation);
    }

    private void traiterAvecIa(Reclamation reclamation, String dernierMessage, Instant horodatage) {
        ReclamationIaResponse reponseIa = reclamationIaClient.traiter(requeteIa(reclamation, dernierMessage));

        if (reponseIa.reponseAutomatiqueValide()) {
            reclamation.getMessages().add(message(reclamation, AuteurMessage.ASSISTANT, reponseIa.reponse(), horodatage));
            reclamation.setStatut(StatutReclamation.REPONDUE_IA);
        } else {
            reclamation.getMessages().add(message(reclamation, AuteurMessage.ASSISTANT, MESSAGE_ESCALADE, horodatage));
            reclamation.setStatut(StatutReclamation.EN_ATTENTE_ADMIN);
        }
        reclamation.setMajLe(horodatage);
    }

    private ReclamationIaRequest requeteIa(Reclamation reclamation, String dernierMessage) {
        List<ReclamationIaRequest.HistoriqueMessage> historique = reclamation.getMessages().stream()
                .map(m -> new ReclamationIaRequest.HistoriqueMessage(
                        m.getAuteur().name(),
                        m.getTexte(),
                        m.getEnvoyeLe() == null ? null : m.getEnvoyeLe().toString()))
                .toList();

        return new ReclamationIaRequest(
                reclamation.getId() == null ? null : String.valueOf(reclamation.getId()),
                reclamation.getClient() == null ? null : reclamation.getClient().getNom(),
                reclamation.getClient() == null ? null : reclamation.getClient().getEmail(),
                reclamation.getCompagnie() == null ? null : reclamation.getCompagnie().getNom(),
                reclamation.getSujet(),
                reclamation.getStatut() == null ? null : reclamation.getStatut().name(),
                dernierMessage,
                historique
        );
    }

    /**
     * Determine la compagnie visee par la reclamation :
     * 1) si le nom d'une compagnie connue est cite dans le message ;
     * 2) sinon, la compagnie de la derniere reservation du client ;
     * 3) sinon, aucune (la reclamation reste orpheline, visible par aucune compagnie).
     */
    private CompagnieProfile resoudreCompagnie(User client, String message) {
        String texte = message == null ? "" : message.toLowerCase(java.util.Locale.FRENCH);

        List<CompagnieProfile> compagnies = compagnieProfileRepository.findAll();
        for (CompagnieProfile compagnie : compagnies) {
            if (compagnie.getNom() != null
                    && !texte.isBlank()
                    && texte.contains(compagnie.getNom().toLowerCase(java.util.Locale.FRENCH))) {
                return compagnie;
            }
        }

        return reservationRepository.findByClientOrderByCreeLeDesc(client).stream()
                .map(Reservation::getCompagnie)
                .filter(nom -> nom != null && !nom.isBlank())
                .findFirst()
                .flatMap(compagnieProfileRepository::findByNomIgnoreCase)
                .orElse(null);
    }

    private Reclamation reclamationDuClient(Authentication authentication, Long id) {
        User client = getUser(authentication);
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reclamation introuvable"));
        if (!reclamation.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reclamation inaccessible");
        }
        return reclamation;
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private static MessageReclamation message(Reclamation reclamation, AuteurMessage auteur, String texte, Instant envoyeLe) {
        return MessageReclamation.builder()
                .reclamation(reclamation)
                .auteur(auteur)
                .texte(texte == null ? "" : texte.trim())
                .envoyeLe(envoyeLe)
                .build();
    }

    private static String sujetDepuis(String message) {
        String texte = message == null || message.isBlank() ? "Reclamation" : message.trim();
        return texte.length() <= 60 ? texte : texte.substring(0, 57) + "...";
    }
}
