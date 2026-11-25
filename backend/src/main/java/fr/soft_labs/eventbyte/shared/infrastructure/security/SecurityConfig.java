package fr.soft_labs.eventbyte.shared.infrastructure.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
 * GET /events/liked, DELETE /events/{id}/like, /bookings/**.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Le frontend (autre origine : localhost:5173) appelle l'API depuis le navigateur.
                // Sans CORS, le navigateur bloque toutes ses requêtes. Utilise le bean corsConfigurationSource.
                .cors(Customizer.withDefaults())
                // API stateless avec token : pas de cookie de session, donc pas de CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Route de test (voir PingController) : 401 sans JWT valide.
                        .requestMatchers("/ping/private").authenticated()
                        // US-8 : liker un événement.
                        .requestMatchers(HttpMethod.POST, "/events/*/like").authenticated()
                        // Tout le reste est public pour l'instant.
                        .anyRequest().permitAll())
                // Valide le JWT s'il est présent dans l'en-tête Authorization: Bearer.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }

    /**
     * Origines autorisées à appeler l'API depuis un navigateur.
     * Configurable via eventbyte.cors.allowed-origins (application.yaml).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${eventbyte.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
