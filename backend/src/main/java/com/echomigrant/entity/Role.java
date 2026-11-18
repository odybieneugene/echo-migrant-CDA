package com.echomigrant.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Role {

    public static final String ADMIN = "admin";
    public static final String JOURNALISTE = "journaliste";
    public static final String REDACTEUR = "redacteur";
    public static final String LECTEUR = "lecteur";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    private String description;

    public Role(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }
}
