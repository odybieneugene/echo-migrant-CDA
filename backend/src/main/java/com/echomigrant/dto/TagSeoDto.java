package com.echomigrant.dto;

import com.echomigrant.entity.TagSeo;
import jakarta.validation.constraints.NotBlank;

public record TagSeoDto(Long id, @NotBlank(message = "Le mot-clé est obligatoire") String motCle) {
    public static TagSeoDto from(TagSeo t) {
        return new TagSeoDto(t.getId(), t.getMotCle());
    }
}
