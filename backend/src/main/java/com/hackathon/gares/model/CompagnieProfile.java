package com.hackathon.gares.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compagnie_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompagnieProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 1200)
    private String description;

    @Column(length = 2000)
    private String logoUrl;

    @Column(length = 2000)
    private String imageCouvertureUrl;

    @ElementCollection
    @CollectionTable(name = "compagnie_galerie_images", joinColumns = @JoinColumn(name = "compagnie_id"))
    @Column(name = "image_url", length = 4000)
    @Builder.Default
    private List<String> galerieImages = new ArrayList<>();

    @Column(length = 500)
    private String localisation;

    @ElementCollection
    @CollectionTable(name = "compagnie_gares_desservies", joinColumns = @JoinColumn(name = "compagnie_id"))
    @Column(name = "gare")
    @Builder.Default
    private List<String> garesDesservies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "compagnie_services", joinColumns = @JoinColumn(name = "compagnie_id"))
    @Column(name = "service")
    @Builder.Default
    private List<String> flotte = new ArrayList<>();

    private double noteMoyenne;
    private int nombreAvis;
}
