package com.hackathon.gares.service;

import com.hackathon.gares.dto.ReservationCompagnieDto;
import com.hackathon.gares.dto.TrajetDto;
import com.hackathon.gares.dto.TrajetRequest;
import com.hackathon.gares.dto.VitrineDto;
import com.hackathon.gares.dto.VitrineRequest;
import com.hackathon.gares.model.*;
import com.hackathon.gares.repository.CompagnieProfileRepository;
import com.hackathon.gares.repository.ReservationRepository;
import com.hackathon.gares.repository.TrajetRepository;
import com.hackathon.gares.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompagnieService {

    private final CompagnieProfileRepository compagnieRepository;
    private final TrajetRepository trajetRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final MetierMapper mapper;

   @Transactional(readOnly = true)
    public VitrineDto obtenirVitrine(String compagnie) {
        return mapper.toVitrineDto(trouverCompagniePublique(compagnie));
    }

    @Transactional(readOnly = true)
    public List<TrajetDto> listerTrajetsPublics(String compagnie) {
        CompagnieProfile profile = trouverCompagniePublique(compagnie);
        return trajetRepository.findByCompagnieAndStatutOrderByDateAscHeureDepartAsc(profile, StatutTrajet.ACTIF)
                .stream()
                .map(mapper::toTrajetDto)
                .toList();
    }

    private CompagnieProfile trouverCompagniePublique(String compagnie) {
        return compagnieRepository.findBySlug(normaliser(compagnie))
                .or(() -> compagnieRepository.findByNomIgnoreCase(compagnie))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compagnie introuvable"));
    }

    @Transactional
    public VitrineDto enregistrerVitrine(Authentication authentication, VitrineRequest request) {
        CompagnieProfile profile = getCompagnieConnectee(authentication);
        if (request.compagnie() != null && !request.compagnie().isBlank()) {
            profile.setNom(request.compagnie().trim());
            profile.setSlug(slugUnique(profile.getNom(), profile.getId()));
        }
        profile.setDescription(request.description());
        profile.setLogoUrl(request.logoUrl());
        profile.setImageCouvertureUrl(request.imageCouvertureUrl());
        profile.setGalerieImages(liste(request.galerieImages()));
        profile.setLocalisation(request.localisation());
        profile.setGaresDesservies(liste(request.garesDesservies()));
        profile.setFlotte(liste(request.flotte()));
        return mapper.toVitrineDto(compagnieRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<TrajetDto> listerTrajets(Authentication authentication) {
        return trajetRepository.findByCompagnieOrderByDateAscHeureDepartAsc(getCompagnieConnectee(authentication))
                .stream()
                .map(mapper::toTrajetDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationCompagnieDto> listerReservations(Authentication authentication) {
        CompagnieProfile compagnie = getCompagnieConnectee(authentication);
        List<Reservation> reservations = reservationRepository.findForCompagnieDashboard(compagnie, compagnie.getNom());

        return reservations.stream()
                .map(this::toReservationCompagnieDto)
                .toList();
    }

    @Transactional
    public TrajetDto creerTrajet(Authentication authentication, TrajetRequest request) {
        CompagnieProfile compagnie = getCompagnieConnectee(authentication);
        Trajet trajet = Trajet.builder()
                .compagnie(compagnie)
                .codeGareDepart(request.codeGareDepart())
                .codeGareArrivee(request.codeGareArrivee())
                .villeDepart(requis(request.villeDepart(), "Ville de depart requise"))
                .villeArrivee(requis(request.villeArrivee(), "Ville d'arrivee requise"))
                .date(parseDate(request.date()))
                .heureDepart(requis(request.heureDepart(), "Heure de depart requise"))
                .duree(request.duree() == null || request.duree().isBlank() ? "4h00" : request.duree())
                .prix(request.prix())
                .placesDisponibles(request.placesDisponibles())
                .statut(StatutTrajet.ACTIF)
                .build();
        return mapper.toTrajetDto(trajetRepository.save(trajet));
    }

    @Transactional
    public TrajetDto modifierTrajet(Authentication authentication, Long id, TrajetRequest request) {
        Trajet trajet = trajetDeLaCompagnie(authentication, id);
        trajet.setCodeGareDepart(request.codeGareDepart());
        trajet.setCodeGareArrivee(request.codeGareArrivee());
        trajet.setVilleDepart(requis(request.villeDepart(), "Ville de depart requise"));
        trajet.setVilleArrivee(requis(request.villeArrivee(), "Ville d'arrivee requise"));
        trajet.setDate(parseDate(request.date()));
        trajet.setHeureDepart(requis(request.heureDepart(), "Heure de depart requise"));
        trajet.setDuree(request.duree() == null || request.duree().isBlank() ? "4h00" : request.duree());
        trajet.setPrix(request.prix());
        trajet.setPlacesDisponibles(request.placesDisponibles());
        return mapper.toTrajetDto(trajet);
    }

    @Transactional
    public void supprimerTrajet(Authentication authentication, Long id) {
        trajetRepository.delete(trajetDeLaCompagnie(authentication, id));
    }

    @Transactional
    public TrajetDto basculerStatutTrajet(Authentication authentication, Long id) {
        Trajet trajet = trajetDeLaCompagnie(authentication, id);
        trajet.setStatut(trajet.getStatut() == StatutTrajet.ACTIF ? StatutTrajet.SUSPENDU : StatutTrajet.ACTIF);
        return mapper.toTrajetDto(trajet);
    }

    public CompagnieProfile getCompagnieConnectee(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        return compagnieRepository.findByUser(user)
                .orElseGet(() -> compagnieRepository.save(CompagnieProfile.builder()
                        .user(user)
                        .nom(user.getNom())
                        .slug(slugUnique(user.getNom(), null))
                        .description("Compagnie de transport interurbain.")
                        .build()));
    }

    private Trajet trajetDeLaCompagnie(Authentication authentication, Long id) {
        CompagnieProfile compagnie = getCompagnieConnectee(authentication);
        Trajet trajet = trajetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trajet introuvable"));
        if (!trajet.getCompagnie().getId().equals(compagnie.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce trajet appartient a une autre compagnie");
        }
        return trajet;
    }

    private ReservationCompagnieDto toReservationCompagnieDto(Reservation reservation) {
        return new ReservationCompagnieDto(
                String.valueOf(reservation.getId()),
                reservation.getClient() == null ? "Client" : reservation.getClient().getNom(),
                reservation.getVilleDepart() + " -> " + reservation.getVilleArrivee(),
                reservation.getDate() == null ? null : reservation.getDate().toString(),
                reservation.getNombreTickets(),
                reservation.getPrix(),
                reservation.getStatut(),
                reservation.getStatutPaiement()
        );
    }

    public static String normaliser(String valeur) {
        String base = valeur == null || valeur.isBlank() ? "compagnie" : valeur.trim();
        return Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public String slugUnique(String nom, Long idExistant) {
        String base = normaliser(nom);
        String slug = base;
        int suffixe = 2;
        while (compagnieRepository.findBySlug(slug)
                .filter(c -> idExistant == null || !c.getId().equals(idExistant))
                .isPresent()) {
            slug = base + "-" + suffixe++;
        }
        return slug;
    }

    private static List<String> liste(List<String> valeurs) {
        return valeurs == null ? List.of() : valeurs.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
    }

    private static LocalDate parseDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date);
    }

    private static String requis(String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return valeur.trim();
    }
}
