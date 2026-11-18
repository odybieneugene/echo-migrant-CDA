package com.echomigrant.service;

import com.echomigrant.dto.UtilisateurDto;
import com.echomigrant.entity.Role;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.BadRequestException;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.RoleRepository;
import com.echomigrant.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administration des comptes (réservée à l'admin, voir UtilisateurController).
 * Garde-fous : un admin ne peut ni se retirer son rôle admin ni supprimer son propre compte,
 * et il reste toujours au moins un administrateur.
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<UtilisateurDto> findAll() {
        return utilisateurRepository.findAll(Sort.by("id")).stream().map(UtilisateurDto::from).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurDto findById(Long id) {
        return UtilisateurDto.from(getOrThrow(id));
    }

    @Transactional
    public UtilisateurDto updateRoles(Long id, Set<String> nomsRoles, Utilisateur adminConnecte) {
        Utilisateur cible = getOrThrow(id);

        Set<Role> nouveauxRoles = new HashSet<>();
        for (String nom : nomsRoles) {
            nouveauxRoles.add(roleRepository.findByNomIgnoreCase(nom.trim())
                .orElseThrow(() -> new BadRequestException("Rôle inconnu : " + nom)));
        }

        boolean perdAdmin = cible.hasRole(Role.ADMIN)
            && nouveauxRoles.stream().noneMatch(r -> r.getNom().equalsIgnoreCase(Role.ADMIN));
        if (perdAdmin) {
            if (cible.getId().equals(adminConnecte.getId())) {
                throw new BadRequestException("Vous ne pouvez pas retirer votre propre rôle administrateur");
            }
            verifierQuIlResteUnAutreAdmin();
        }

        cible.setRoles(nouveauxRoles);
        return UtilisateurDto.from(utilisateurRepository.save(cible));
    }

    @Transactional
    public void delete(Long id, Utilisateur adminConnecte) {
        Utilisateur cible = getOrThrow(id);
        if (cible.getId().equals(adminConnecte.getId())) {
            throw new BadRequestException("Vous ne pouvez pas supprimer votre propre compte");
        }
        if (cible.hasRole(Role.ADMIN)) {
            verifierQuIlResteUnAutreAdmin();
        }
        utilisateurRepository.delete(cible);
    }

    // Défense en profondeur : l'admin connecté compte déjà pour un, mais la règle reste vraie
    // même si le contrôle "pas soi-même" venait à changer (ou en cas de requêtes concurrentes).
    private void verifierQuIlResteUnAutreAdmin() {
        if (utilisateurRepository.countByRoles_NomIgnoreCase(Role.ADMIN) <= 1) {
            throw new BadRequestException("Il doit rester au moins un administrateur");
        }
    }

    private Utilisateur getOrThrow(Long id) {
        return utilisateurRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", id));
    }
}
