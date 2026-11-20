package com.echomigrant.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Une page de résultats. On n'expose pas directement le Page de Spring Data :
 * son format JSON n'est pas garanti stable d'une version à l'autre.
 *
 * @param page numéro de la page, à partir de 0
 */
public record PageResponse<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }
}
