package com.hackathon.gares.service;

import com.hackathon.gares.dto.AuthResponse;
import com.hackathon.gares.dto.ForgotPasswordRequest;
import com.hackathon.gares.dto.ForgotPasswordResponse;
import com.hackathon.gares.dto.LoginRequest;
import com.hackathon.gares.dto.RegisterRequest;
import com.hackathon.gares.dto.RegisterResponse;
import com.hackathon.gares.dto.ResetPasswordRequest;
import com.hackathon.gares.dto.ResetPasswordResponse;
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
import java.util.Base64;

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

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private static final long RESET_TOKEN_VALIDITE_SECONDES = 30 * 60;

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
        // Flux de reinitialisation standard : on ne change jamais le mot de passe ici.
        // On genere un jeton a duree limitee, on l'enregistre puis on envoie par email
        // un lien vers la page de reinitialisation du site. La reponse reste generique
        // pour ne pas reveler si l'email existe (pas d'enumeration de comptes).
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = genererTokenReinitialisation();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiresAt(Instant.now().plusSeconds(RESET_TOKEN_VALIDITE_SECONDES));
            userRepository.save(user);
            emailVerificationService.envoyerReinitialisation(user, lienReinitialisation(token));
        });

        return new ForgotPasswordResponse(
                "Si un compte existe pour cet email, un lien de reinitialisation vient d'etre envoye."
        );
    }

    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .filter(u -> u.getPasswordResetExpiresAt() != null
                        && u.getPasswordResetExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Lien de reinitialisation invalide ou expire. Refais une demande."));

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        return new ResetPasswordResponse("Mot de passe reinitialise. Tu peux maintenant te connecter.");
    }

    private String genererTokenReinitialisation() {
        byte[] octets = new byte[32];
        secureRandom.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private String lienReinitialisation(String token) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
        return base + "/reinitialiser-mot-de-passe?token=" + token;
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
