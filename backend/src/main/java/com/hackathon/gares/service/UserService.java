package com.hackathon.gares.service;

import com.hackathon.gares.dto.AccountResponse;
import com.hackathon.gares.dto.AuthResponse;
import com.hackathon.gares.dto.UpdateAccountRequest;
import com.hackathon.gares.exception.EmailDejaUtiliseException;
import com.hackathon.gares.model.Role;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.CompagnieProfileRepository;
import com.hackathon.gares.repository.UserRepository;
import com.hackathon.gares.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CompagnieProfileRepository compagnieProfileRepository;
    private final CompagnieService compagnieService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Transactional(readOnly = true)
    public AccountResponse obtenirCompte(Authentication authentication) {
        return toAccountResponse(getUser(authentication));
    }

    @Transactional
    public AuthResponse mettreAJourCompte(Authentication authentication, UpdateAccountRequest request) {
        User user = getUser(authentication);

        // On conserve l'email tel que saisi (comme a l'inscription) : l'authentification
        // est sensible a la casse, lowercaser ici casserait la connexion.
        String nouvelEmail = request.email().trim();
        if (!nouvelEmail.equals(user.getEmail()) && userRepository.existsByEmail(nouvelEmail)) {
            throw new EmailDejaUtiliseException(nouvelEmail);
        }

        user.setNom(request.nom().trim());
        user.setEmail(nouvelEmail);

        appliquerChangementMotDePasse(user, request);

        userRepository.save(user);
        synchroniserNomCompagnie(user);

        // Le sujet du JWT est l'email : s'il change, l'ancien jeton devient invalide.
        // On renvoie donc un jeton frais pour garder l'utilisateur connecte.
        String token = jwtUtil.generateToken(user, jwtExpirationMs);
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getRole(),
                jwtUtil.expirationFor(jwtExpirationMs).toString()
        );
    }

    private void synchroniserNomCompagnie(User user) {
        // Pour une compagnie, le nom du compte est aussi le nom public de la vitrine.
        // On le garde synchronise (nom + slug) pour ne pas casser l'acces a la vitrine.
        if (user.getRole() != Role.COMPAGNIE) {
            return;
        }
        compagnieProfileRepository.findByUser(user).ifPresent(profile -> {
            profile.setNom(user.getNom());
            profile.setSlug(compagnieService.slugUnique(user.getNom(), profile.getId()));
            compagnieProfileRepository.save(profile);
        });
    }

    private void appliquerChangementMotDePasse(User user, UpdateAccountRequest request) {
        String nouveau = request.nouveauMotDePasse();
        if (nouveau == null || nouveau.isBlank()) {
            return;
        }
        if (request.motDePasseActuel() == null
                || !passwordEncoder.matches(request.motDePasseActuel(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe actuel incorrect");
        }
        if (nouveau.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit contenir au moins 8 caractères");
        }
        user.setPasswordHash(passwordEncoder.encode(nouveau));
    }

    private AccountResponse toAccountResponse(User user) {
        return new AccountResponse(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getRole(),
                Boolean.TRUE.equals(user.getEmailVerified())
        );
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }
}
