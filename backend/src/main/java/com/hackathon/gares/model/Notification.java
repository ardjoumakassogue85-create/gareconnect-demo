package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    @Column(nullable = false)
    private String titre;

    @Column(length = 500)
    private String message;

    /** Reservation concernee (pour un rappel de notation). */
    private Long reservationId;

    private String compagnie;

    @Builder.Default
    private boolean lu = false;

    private Instant creeLe;
}
