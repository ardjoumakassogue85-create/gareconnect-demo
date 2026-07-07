package com.hackathon.gares.service;

import com.hackathon.gares.dto.ReclamationDto;
import com.hackathon.gares.dto.ReclamationStatutRequest;
import com.hackathon.gares.model.*;
import com.hackathon.gares.repository.ReclamationRepository;
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

    private final ReclamationRepository reclamationRepository;
    private final UserRepository userRepository;
    private final MetierMapper mapper;

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
                .sujet(sujetDepuis(message))
                .statut(StatutReclamation.REPONDUE_IA)
                .creeLe(maintenant)
                .majLe(maintenant)
                .build();
        reclamation.getMessages().add(message(reclamation, AuteurMessage.CLIENT, message, maintenant));
        reclamation.getMessages().add(message(reclamation, AuteurMessage.ASSISTANT,
                "Votre reclamation est enregistree. Un agent pourra la reprendre si besoin.", maintenant.plusMillis(1)));
        return mapper.toReclamationDto(reclamationRepository.save(reclamation));
    }

    @Transactional
    public ReclamationDto ajouterMessage(Authentication authentication, Long id, String texte) {
        Reclamation reclamation = reclamationDuClient(authentication, id);
        Instant maintenant = Instant.now();
        reclamation.getMessages().add(message(reclamation, AuteurMessage.CLIENT, texte, maintenant));
        reclamation.setStatut(StatutReclamation.EN_ATTENTE_ADMIN);
        reclamation.setMajLe(maintenant);
        return mapper.toReclamationDto(reclamation);
    }

    @Transactional(readOnly = true)
    public List<ReclamationDto> listerPourCompagnie() {
        return reclamationRepository.findAllByOrderByMajLeDesc().stream()
                .map(mapper::toReclamationDto)
                .toList();
    }

    @Transactional
    public ReclamationDto repondre(Long id, ReclamationStatutRequest request) {
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reclamation introuvable"));
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
