package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "trajets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trajet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compagnie_id")
    private CompagnieProfile compagnie;

    private String codeGareDepart;
    private String codeGareArrivee;

    @Column(nullable = false)
    private String villeDepart;

    @Column(nullable = false)
    private String villeArrivee;

    private LocalDate date;

    @Column(nullable = false)
    private String heureDepart;

    @Builder.Default
    private String duree = "4h00";

    private int prix;
    private int placesDisponibles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutTrajet statut = StatutTrajet.ACTIF;

    /**
     * Combine la date et l'heure de depart en un instant local.
     * Retourne null si la date est absente (trajet a date flexible).
     */
    public LocalDateTime dateHeureDepart() {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, parseHeure(heureDepart));
    }

    /** Vrai si le depart (date + heure) est deja passe. */
    public boolean estExpire() {
        LocalDateTime depart = dateHeureDepart();
        return depart != null && depart.isBefore(LocalDateTime.now());
    }

    private static LocalTime parseHeure(String heure) {
        if (heure == null || heure.isBlank()) {
            return LocalTime.MIDNIGHT;
        }
        try {
            return LocalTime.parse(heure.trim());
        } catch (Exception ignore) {
            // Heure non reconnue : on considere minuit pour ne l'expirer qu'apres la journee.
            return LocalTime.MIDNIGHT;
        }
    }
}
