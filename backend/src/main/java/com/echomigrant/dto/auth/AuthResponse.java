package com.echomigrant.dto.auth;

import com.echomigrant.dto.UtilisateurDto;

public record AuthResponse(
    String token,
    UtilisateurDto user
) {
}
