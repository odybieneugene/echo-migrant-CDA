package com.echomigrant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "medias")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 500)
    private String url;

    private String description;

    @JsonIgnore
    @ManyToMany(mappedBy = "medias")
    private Set<Article> articles = new HashSet<>();
}
