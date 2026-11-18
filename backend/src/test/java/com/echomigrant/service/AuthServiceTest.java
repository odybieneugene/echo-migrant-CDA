package com.echomigrant.service;

import com.echomigrant.dto.auth.AuthResponse;
import com.echomigrant.dto.auth.RegisterRequest;
import com.echomigrant.entity.Role;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.BadRequestException;
import com.echomigrant.repository.RoleRepository;
import com.echomigrant.repository.UtilisateurRepository;
import com.echomigrant.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Role lecteurRole;

    @BeforeEach
    void setUp() {
        lecteurRole = new Role(Role.LECTEUR, "Lecteur");
    }

    @Test
    void register_createsUserWithDefaultRole_andReturnsToken() {
        RegisterRequest request = new RegisterRequest("Bien Eugene", "Ody", "ody@example.com", "secret123", null);

        when(utilisateurRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByNomIgnoreCase(Role.LECTEUR)).thenReturn(Optional.of(lecteurRole));
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(request.email())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.user().email()).isEqualTo("ody@example.com");
        assertThat(response.user().roles()).contains(Role.LECTEUR);
        verify(utilisateurRepository).save(any(Utilisateur.class));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Nom", "Prenom", "existe@example.com", "secret123", null);
        when(utilisateurRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("existe déjà");

        verify(utilisateurRepository, never()).save(any());
    }
}
