package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * File virtuelle : fenetre d'arrivee attribuee a un voyageur pour un depart donne.
 * En repartissant les fenetres d'arrivee, on lisse le pic de presence en gare.
 */
@Entity
@Table(name = "creneaux_arrivee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreneauArrivee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String gare;

    private LocalDate dateVoyage;

    /** Debut de la fenetre d'arrivee, format "HH:mm". */
    @Column(nullable = false)
    private String fenetre;

    private Long trajetId;

    private Instant creeLe;
}
