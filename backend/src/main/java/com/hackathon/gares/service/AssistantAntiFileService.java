package com.hackathon.gares.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hackathon.gares.dto.AffluenceGareDto;
import com.hackathon.gares.dto.ConseilAntiFileRequest;
import com.hackathon.gares.dto.ConseilAntiFileResponse;
import com.hackathon.gares.dto.CreneauAffluenceDto;
import com.hackathon.gares.dto.CriteresRecherche;
import com.hackathon.gares.dto.TrajetDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Assistant "anti-file d'attente".
 *
 * Architecture volontaire : un COEUR DETERMINISTE choisit et classe les departs
 * par "cout de file" (affluence prevue en gare + proximite a l'heure souhaitee),
 * puis l'IA (Gemini) choisit parmi ces trajets REELS et redige un conseil humain.
 * L'IA ne peut donc jamais halluciner un trajet inexistant, et si elle est
 * indisponible on retombe integralement sur le deterministe.
 */
@Service
@RequiredArgsConstructor
public class AssistantAntiFileService {

    private final AssistantIaService assistantIaService;
    private final VoyageurService voyageurService;
    private final AffluenceService affluenceService;
    private final GeminiClient geminiClient;

    // Poids de la proximite horaire face a l'affluence (0.25 pt par minute d'ecart).
    private static final double POIDS_PROXIMITE = 0.25;

    public ConseilAntiFileResponse conseiller(ConseilAntiFileRequest request) {
        CriteresRecherche criteres = assistantIaService.extraireCriteresRecherche(request.texteLibre());
        String villeDepart = premier(criteres.villeDepart(), request.villeDepart());
        String villeArrivee = premier(criteres.villeArrivee(), request.villeArrivee());
        LocalDate date = criteres.date() != null ? criteres.date() : parseDate(request.date());
        String heurePref = criteres.heureDepart();

        List<TrajetDto> trajets = voyageurService.rechercher(
                villeDepart, villeArrivee, date == null ? null : date.toString());
        List<TrajetDto> disponibles = trajets.stream().filter(t -> t.placesDisponibles() > 0).toList();
        List<TrajetDto> base = disponibles.isEmpty() ? trajets : disponibles;

        if (base.isEmpty()) {
            return aucunTrajet(villeDepart, villeArrivee);
        }

        AffluenceGareDto profil = (villeDepart == null || villeDepart.isBlank())
                ? null
                : affluenceService.affluenceGare(villeDepart, date);

        // Classement deterministe du plus calme au plus charge.
        List<TrajetDto> classes = base.stream()
                .sorted(Comparator.comparingDouble(t -> coutFile(t, profil, heurePref)))
                .toList();
        TrajetDto recommandeDeterministe = classes.get(0);

        // Surcouche IA : choix + reformulation, valides contre la liste reelle.
        ChoixIa choix = reformulerAvecIa(request, villeDepart, villeArrivee, date, classes, profil, heurePref);
        TrajetDto recommande = choix.trajet() != null ? choix.trajet() : recommandeDeterministe;

        String niveau = niveauA(profil, recommande.heureDepart());
        String heureArrivee = affluenceService.conseilArrivee(recommande.heureDepart(), niveau);
        List<TrajetDto> alternatives = classes.stream()
                .filter(t -> !t.id().equals(recommande.id()))
                .limit(2)
                .toList();

        String message = choix.message() != null
                ? choix.message()
                : messageDeterministe(recommande, niveau, heureArrivee, villeDepart, alternatives, profil);
        String resume = choix.resume() != null
                ? choix.resume()
                : "Prends le départ de " + recommande.heureDepart() + ", arrive vers " + heureArrivee + ".";

        return new ConseilAntiFileResponse(
                message, resume, recommande, heureArrivee, niveau, alternatives,
                choix.trajet() != null ? "IA" : "DETERMINISTE");
    }

    // --- Coeur deterministe ---

    private double coutFile(TrajetDto trajet, AffluenceGareDto profil, String heurePref) {
        double affluence = scoreA(profil, trajet.heureDepart());
        double proximite = 0;
        if (heurePref != null && !heurePref.isBlank()) {
            proximite = Math.abs(minutes(trajet.heureDepart()) - minutes(heurePref));
        }
        return affluence + POIDS_PROXIMITE * proximite;
    }

    private int scoreA(AffluenceGareDto profil, String heureDepart) {
        return creneau(profil, heureDepart).map(CreneauAffluenceDto::score).orElse(50);
    }

    private String niveauA(AffluenceGareDto profil, String heureDepart) {
        return creneau(profil, heureDepart).map(CreneauAffluenceDto::niveau).orElse("MOYENNE");
    }

    private Optional<CreneauAffluenceDto> creneau(AffluenceGareDto profil, String heureDepart) {
        if (profil == null || heureDepart == null || heureDepart.length() < 2) {
            return Optional.empty();
        }
        String cle = heureDepart.substring(0, 2) + ":00";
        return profil.creneaux().stream().filter(c -> c.heure().equals(cle)).findFirst();
    }

    // --- Surcouche IA ---

    private ChoixIa reformulerAvecIa(ConseilAntiFileRequest request, String villeDepart, String villeArrivee,
                                     LocalDate date, List<TrajetDto> classes, AffluenceGareDto profil, String heurePref) {
        if (!geminiClient.estActif()) {
            return ChoixIa.vide();
        }

        List<TrajetDto> candidats = classes.stream().limit(5).toList();
        Optional<JsonNode> reponse = geminiClient.genererJson(prompt(request, villeDepart, villeArrivee, date, candidats, profil));
        if (reponse.isEmpty()) {
            return ChoixIa.vide();
        }

        JsonNode json = reponse.get();
        String trajetId = texte(json, "trajetId");
        TrajetDto choisi = candidats.stream().filter(t -> t.id().equals(trajetId)).findFirst().orElse(null);
        return new ChoixIa(choisi, texte(json, "message"), texte(json, "resume"));
    }

    private String prompt(ConseilAntiFileRequest request, String villeDepart, String villeArrivee,
                          LocalDate date, List<TrajetDto> candidats, AffluenceGareDto profil) {
        StringBuilder options = new StringBuilder();
        for (TrajetDto t : candidats) {
            String niveau = niveauA(profil, t.heureDepart());
            String arrivee = affluenceService.conseilArrivee(t.heureDepart(), niveau);
            options.append("- id=").append(t.id())
                    .append(", depart ").append(t.heureDepart())
                    .append(", affluence ").append(niveau).append(" (").append(scoreA(profil, t.heureDepart())).append("/100)")
                    .append(", ").append(t.placesDisponibles()).append(" places")
                    .append(", ").append(t.prix()).append(" FCFA")
                    .append(", arriver vers ").append(arrivee)
                    .append(", ").append(t.compagnie())
                    .append("\n");
        }

        String demande = (request.texteLibre() == null || request.texteLibre().isBlank())
                ? "Trouver le meilleur depart pour eviter la file d'attente."
                : request.texteLibre();

        return """
                Tu es l'assistant "anti-file d'attente" d'une plateforme de gares routieres en Cote d'Ivoire.
                Ton objectif : aider le voyageur a eviter la file d'attente en gare.
                Demande du voyageur : "%s".
                Gare de depart : %s. Destination : %s. Date : %s.

                Departs disponibles (deja tries du plus calme au plus charge) :
                %s
                Choisis LE depart qui minimise l'attente tout en respectant la demande du voyageur
                (une heure plus calme evite la file). Reponds UNIQUEMENT en JSON :
                {"trajetId":"<un id EXACT de la liste>","message":"<2 a 3 phrases chaleureuses en francais : quel depart prendre, a quelle heure arriver a la gare, et pourquoi cela evite la file>","resume":"<une phrase courte>"}
                """.formatted(
                demande,
                villeDepart == null ? "?" : villeDepart,
                villeArrivee == null ? "?" : villeArrivee,
                date == null ? "prochainement" : date.toString(),
                options
        );
    }

    // --- Messages deterministes (repli) ---

    private String messageDeterministe(TrajetDto trajet, String niveau, String heureArrivee,
                                       String ville, List<TrajetDto> alternatives, AffluenceGareDto profil) {
        String gare = ville == null || ville.isBlank() ? "la gare" : "la gare de " + ville;
        StringBuilder message = new StringBuilder();
        message.append("Pour éviter la file, prends le départ de ").append(trajet.heureDepart())
                .append(" : affluence ").append(niveau.toLowerCase()).append(" à ").append(gare)
                .append(", ").append(trajet.placesDisponibles()).append(" place(s) libre(s). ")
                .append("Arrive vers ").append(heureArrivee).append(".");
        if (!alternatives.isEmpty()) {
            message.append(" Sinon, le départ de ").append(alternatives.get(0).heureDepart())
                    .append(" est une bonne alternative.");
        }
        return message.toString();
    }

    private ConseilAntiFileResponse aucunTrajet(String villeDepart, String villeArrivee) {
        String route = (villeDepart == null ? "?" : villeDepart) + " → " + (villeArrivee == null ? "?" : villeArrivee);
        return new ConseilAntiFileResponse(
                "Aucun départ disponible pour " + route + " à cette date. Essaie une autre date ou une autre destination.",
                "Aucun départ disponible.",
                null, null, null, List.of(), "DETERMINISTE");
    }

    // --- Utilitaires ---

    private static String premier(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static LocalDate parseDate(String date) {
        try {
            return date == null || date.isBlank() ? null : LocalDate.parse(date);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static int minutes(String heure) {
        try {
            String[] p = heure.split(":");
            return Integer.parseInt(p[0]) * 60 + (p.length > 1 ? Integer.parseInt(p[1]) : 0);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private static String texte(JsonNode node, String champ) {
        JsonNode v = node.path(champ);
        return v.isMissingNode() || v.isNull() || v.asText("").isBlank() ? null : v.asText().trim();
    }

    private record ChoixIa(TrajetDto trajet, String message, String resume) {
        static ChoixIa vide() {
            return new ChoixIa(null, null, null);
        }
    }
}
