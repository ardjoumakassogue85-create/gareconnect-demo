package com.hackathon.gares.service;

import com.hackathon.gares.dto.AuthResponse;
import com.hackathon.gares.dto.ForgotPasswordRequest;
import com.hackathon.gares.dto.ForgotPasswordResponse;
import com.hackathon.gares.dto.LoginRequest;
import com.hackathon.gares.dto.RegisterRequest;
import com.hackathon.gares.dto.RegisterResponse;
import com.hackathon.gares.dto.VerifyEmailResponse;
import com.hackathon.gares.exception.EmailDejaUtiliseException;
import com.hackathon.gares.model.CompagnieProfile;
import com.hackathon.gares.model.Role;
import com.hackathon.gares.model.User;
import com.hackathon.gares.repository.CompagnieProfileRepository;
import com.hackathon.gares.repository.UserRepository;
import com.hackathon.gares.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CompagnieProfileRepository compagnieProfileRepository;
    private final EmailVerificationService emailVerificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.remember-me-expiration-ms}")
    private long rememberMeExpirationMs;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nom(request.nom())
                .role(request.role())
                .emailVerified(false)
                .emailVerificationCode(generateVerificationCode())
                .emailVerificationExpiresAt(Instant.now().plusSeconds(15 * 60))
                .build();

        userRepository.save(user);
        emailVerificationService.envoyerVerification(user);

        return new RegisterResponse(
                "Compte cree. Un email de verification a ete envoye.",
                user.getEmail()
        );
    }

    public VerifyEmailResponse verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de verification invalide"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new VerifyEmailResponse("Email deja verifie. Tu peux te connecter.");
        }

        if (user.getEmailVerificationCode() == null
                || !user.getEmailVerificationCode().equals(code.trim())
                || user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de verification invalide ou expire");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);

        creerProfilCompagnieSiNecessaire(user);

        return new VerifyEmailResponse("Email verifie avec succes. Tu peux maintenant te connecter.");
    }

    public RegisterResponse resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun compte a verifier avec cet email"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new RegisterResponse("Cet email est deja verifie. Tu peux te connecter.", user.getEmail());
        }

        user.setEmailVerificationCode(generateVerificationCode());
        user.setEmailVerificationExpiresAt(Instant.now().plusSeconds(15 * 60));
        userRepository.save(user);
        emailVerificationService.envoyerVerification(user);

        return new RegisterResponse(
                "Un nouveau code de verification a ete envoye.",
                user.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après authentification"));

        long expirationMs = Boolean.TRUE.equals(request.rememberMe()) ? rememberMeExpirationMs : jwtExpirationMs;
        String token = jwtUtil.generateToken(user, expirationMs);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getRole(),
                jwtUtil.expirationFor(expirationMs).toString()
        );
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String temporaryPassword = generateTemporaryPassword();

        userRepository.findByEmail(request.email()).ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            userRepository.save(user);
        });

        // En production, ne jamais retourner ce mot de passe dans la reponse :
        // l'envoyer par email via un token de reinitialisation a duree limitee.
        return new ForgotPasswordResponse(
                "Si ce compte existe, un mot de passe temporaire a ete prepare.",
                temporaryPassword
        );
    }

    private String generateTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";
        StringBuilder password = new StringBuilder("Rg-");
        for (int i = 0; i < 10; i++) {
            password.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return password.toString();
    }

    private String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return Integer.toString(code);
    }

    private void creerProfilCompagnieSiNecessaire(User user) {
        if (user.getRole() == Role.COMPAGNIE && compagnieProfileRepository.findByUser(user).isEmpty()) {
            compagnieProfileRepository.save(CompagnieProfile.builder()
                    .user(user)
                    .nom(user.getNom())
                    .slug(slugUnique(user.getNom()))
                    .description("Compagnie de transport interurbain.")
                    .build());
        }
    }

    private String slugUnique(String nom) {
        String base = CompagnieService.normaliser(nom);
        String slug = base;
        int suffixe = 2;
        while (compagnieProfileRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + suffixe++;
        }
        return slug;
    }
}
