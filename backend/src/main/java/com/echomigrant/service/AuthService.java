package com.echomigrant.service;

import com.echomigrant.dto.UtilisateurDto;
import com.echomigrant.dto.auth.AuthResponse;
import com.echomigrant.dto.auth.LoginRequest;
import com.echomigrant.dto.auth.RegisterRequest;
import com.echomigrant.entity.Role;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.BadRequestException;
import com.echomigrant.repository.RoleRepository;
import com.echomigrant.repository.UtilisateurRepository;
import com.echomigrant.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setEmail(request.email());
        utilisateur.setPassword(passwordEncoder.encode(request.password()));

        // Toujours "lecteur" : seul un admin peut attribuer un rôle éditorial ou admin.
        Role role = roleRepository.findByNomIgnoreCase(Role.LECTEUR)
            .orElseThrow(() -> new IllegalStateException("Rôle par défaut 'lecteur' manquant en base"));

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        utilisateur.setRoles(roles);

        utilisateur = utilisateurRepository.save(utilisateur);

        String token = jwtUtil.generateToken(utilisateur.getEmail());
        return new AuthResponse(token, UtilisateurDto.from(utilisateur));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Identifiants invalides");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        String token = jwtUtil.generateToken(utilisateur.getEmail());
        return new AuthResponse(token, UtilisateurDto.from(utilisateur));
    }
}
