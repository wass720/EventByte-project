package fr.soft_labs.eventbyte.shared.infrastructure.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur TEMPORAIRE, uniquement là pour vérifier la sécurité JWT (US-3).
 *
 * - /ping/public  : accessible sans token.
 * - /ping/private : exige un JWT Keycloak valide, sinon 401.
 *
 * À SUPPRIMER (avec la règle "/ping/private" de SecurityConfig) une fois que
 * les vrais contrôleurs protégés existent (likes, réservations...).
 */
@RestController
public class PingController {

    /** Route publique : doit répondre 200 sans token. */
    @GetMapping("/ping/public")
    public String publicPing() {
        return "public ok";
    }

    /**
     * Route protégée : 401 sans token valide.
     * authentication.getName() renvoie le "sub" du JWT, c'est-à-dire l'ID de
     * l'utilisateur Keycloak. C'est cette valeur qu'il faudra utiliser pour
     * identifier l'utilisateur (jamais l'identité envoyée dans le corps de la requête).
     */
    @GetMapping("/ping/private")
    public String privatePing(Authentication authentication) {
        return "private ok, user=" + authentication.getName();
    }
}