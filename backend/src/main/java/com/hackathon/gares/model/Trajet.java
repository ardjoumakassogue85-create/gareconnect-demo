package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
}
