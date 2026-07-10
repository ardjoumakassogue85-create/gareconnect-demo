package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reclamations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compagnie_id")
    private CompagnieProfile compagnie;

    @Column(nullable = false)
    private String sujet;

    @Enumerated(EnumType.STRING)
    private StatutReclamation statut;

    private Instant creeLe;
    private Instant majLe;

    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("envoyeLe ASC")
    @Builder.Default
    private List<MessageReclamation> messages = new ArrayList<>();
}
