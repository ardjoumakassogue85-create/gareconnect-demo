package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "messages_reclamation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReclamation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reclamation_id")
    private Reclamation reclamation;

    @Enumerated(EnumType.STRING)
    private AuteurMessage auteur;

    @Column(length = 2000)
    private String texte;

    private Instant envoyeLe;
}
