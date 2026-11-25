package fr.soft_labs.eventbyte.shared.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de la sécurité (US-3).
 *
 * Le backend est un "resource server" OAuth2 : il ne gère aucun mot de passe.
 * Il vérifie seulement la signature des JWT émis par Keycloak (realm "eventbyte"),
 * grâce à spring.security.oauth2.resourceserver.jwt.issuer-uri (application.yaml).
 *
 * Les règles sont lues de haut en bas : la première qui correspond s'applique.
 * Pour protéger une nouvelle route, ajouter une ligne .requestMatchers(...).authenticated()
 * AVANT .anyRequest().permitAll().
 *
 * TODO : quand les contrôleurs existeront, protéger les routes "auth" du sujet :
 * GET /events/liked, POST et DELETE /events/{id}/like, /bookings/**.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API stateless avec token : pas de cookie de session, donc pas de CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Route de test (voir PingController) : 401 sans JWT valide.
                        .requestMatchers("/ping/private").authenticated()
                        // Tout le reste est public pour l'instant.
                        .anyRequest().permitAll())
                // Valide le JWT s'il est présent dans l'en-tête Authorization: Bearer.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}