package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "avis_gares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvisGare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeGare;
    private int note;

    @Column(length = 1200)
    private String commentaire;
}
