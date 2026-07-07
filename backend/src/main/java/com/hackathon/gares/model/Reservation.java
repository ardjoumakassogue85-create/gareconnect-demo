package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id")
    private Trajet trajet;

    @Column(nullable = false, unique = true)
    private String codeBillet;

    private String codeGareDepart;
    private String codeGareArrivee;
    private String villeDepart;
    private String villeArrivee;
    private LocalDate date;
    private String heure;
    private String compagnie;
    private int prix;
    private int nombreTickets;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;

    @Enumerated(EnumType.STRING)
    private StatutPaiement statutPaiement;

    @Enumerated(EnumType.STRING)
    private MethodePaiement methodePaiement;

    @Enumerated(EnumType.STRING)
    private OperateurMobileMoney operateurMobileMoney;

    private Instant creeLe;
    private Instant annulableJusquA;
    private Integer note;

    @Column(length = 1200)
    private String commentaire;
}
