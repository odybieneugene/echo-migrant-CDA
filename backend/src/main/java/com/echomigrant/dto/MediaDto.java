package com.echomigrant.dto;

import com.echomigrant.entity.Media;
import jakarta.validation.constraints.NotBlank;

public record MediaDto(
    Long id,
    @NotBlank(message = "Le type est obligatoire") String type,
    @NotBlank(message = "L'URL est obligatoire") String url,
    String description
) {
    public static MediaDto from(Media m) {
        return new MediaDto(m.getId(), m.getType(), m.getUrl(), m.getDescription());
    }
}
