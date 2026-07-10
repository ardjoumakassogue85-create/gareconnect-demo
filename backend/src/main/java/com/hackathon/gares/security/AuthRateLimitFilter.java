package com.hackathon.gares.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de requetes sur /api/auth/** par adresse IP (fenetre glissante
 * d'une minute) pour contrer le brute force sur la connexion / le mot de passe
 * oublie. Au-dela du seuil : reponse 429.
 *
 * Implementation volontairement legere (en memoire, sans dependance).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUETES = 20;
    private static final long FENETRE_MS = 60_000;

    private final ConcurrentHashMap<String, Fenetre> compteurs = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        if (limiteAtteinte(cleClient(request))) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"Trop de tentatives. Réessaie dans une minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean limiteAtteinte(String cle) {
        long maintenant = System.currentTimeMillis();
        Fenetre fenetre = compteurs.compute(cle, (k, existante) -> {
            if (existante == null || maintenant - existante.debut > FENETRE_MS) {
                return new Fenetre(maintenant);
            }
            existante.compte++;
            return existante;
        });

        if (compteurs.size() > 5000) {
            compteurs.entrySet().removeIf(entree -> maintenant - entree.getValue().debut > FENETRE_MS);
        }
        return fenetre.compte > MAX_REQUETES;
    }

    private String cleClient(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Fenetre {
        final long debut;
        int compte;

        Fenetre(long debut) {
            this.debut = debut;
            this.compte = 1;
        }
    }
}
