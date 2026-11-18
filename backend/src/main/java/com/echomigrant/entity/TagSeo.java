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
@Table(name = "tag_seos")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class TagSeo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mot_cle", nullable = false, unique = true, length = 100)
    private String motCle;

    @JsonIgnore
    @ManyToMany(mappedBy = "tagsSeo")
    private Set<Article> articles = new HashSet<>();
}
