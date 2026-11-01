
package com.inzinjeri.rs_projekat.config;

import com.inzinjeri.rs_projekat.model.Uloga;
import com.inzinjeri.rs_projekat.repositories.KorisnikRepository;
import com.inzinjeri.rs_projekat.services.AuthService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    public static final String CLAIM_ULOGA = "uloga";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ObjectProvider<KorisnikRepository> korisnici) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/predmet/add",
                                "/api/nalozi/**",
                                "/api/studenti/**", "/api/profesori/**",
                                "/api/rokovi/**", "/api/ucionice/**",
                                "/api/termini/**", "/api/grupe/**",
                                "/api/predaje-predmet",
                                "/api/raspored/**",
                                "/api/zadaci"
                        ).hasRole(Uloga.PROFESOR.name())
                        .requestMatchers(HttpMethod.PUT,
                                "/api/raspored/**",
                                "/api/nalozi/**", "/api/studenti/**", "/api/profesori/**",
                                "/api/rokovi/**", "/api/ucionice/**",
                                "/api/termini/**", "/api/grupe/**",
                                "/api/predmet/**", "/api/predaje-predmet/**", "/api/zadaci/**", "/api/rad/edit/**"
                        ).hasRole(Uloga.PROFESOR.name())
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/raspored/**",
                                "/api/nalozi/**", "/api/studenti/**", "/api/profesori/**",
                                "/api/rokovi/**", "/api/ucionice/**",
                                "/api/termini/**", "/api/grupe/**",
                                "/api/predmet/**", "/api/predaje-predmet/**", "/api/zadaci/**", "/api/rad/**"
                        ).hasRole(Uloga.PROFESOR.name())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(korisnici))));

        return http.build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
            ObjectProvider<KorisnikRepository> korisnici) {
        JwtGrantedAuthoritiesConverter authorities =
                new JwtGrantedAuthoritiesConverter();

        authorities.setAuthoritiesClaimName(CLAIM_ULOGA);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return jwt -> {
            Object id = jwt.getClaim(AuthService.CLAIM_KORISNIK_ID);
            if (!(id instanceof Number broj) || broj.longValue() <= 0) {
                throw new InvalidBearerTokenException("Token nema ispravan identitet naloga");
            }
            korisnici.getObject().findById(broj.longValue())
                    .filter(k -> jwt.getSubject() != null && jwt.getSubject().equals(k.getEmail())
                            && k.getUloga() != null
                            && k.getUloga().name().equals(jwt.getClaim(CLAIM_ULOGA)))
                    .orElseThrow(() -> new InvalidBearerTokenException("Token vise ne odgovara nalogu"));
            return converter.convert(jwt);
        };
    }

    @Bean
    public SecretKey jwtKey(SecurityProperties props) {
        byte[] secret = props.jwtSecret() == null
                ? new byte[0]
                : props.jwtSecret().getBytes(StandardCharsets.UTF_8);

        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt-secret mora imati najmanje 32 bajta");
        }

        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder =
                (DelegatingPasswordEncoder) PasswordEncoderFactories
                        .createDelegatingPasswordEncoder();

        // Podrška za postojeće BCrypt hash-eve bez {bcrypt} prefiksa.
        encoder.setDefaultPasswordEncoderForMatches(
                new BCryptPasswordEncoder());

        return encoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            SecurityProperties props) {

        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOriginPatterns(props.corsOrigins());
        cors.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));
        cors.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", cors);

        return source;
    }
}
