package com.inzinjeri.rs_projekat.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"kratak-kljuc", "1234567890123456789012345678901"})
    void nedostajuciIliPrekratakKljucSeOdbija(String secret) {
        assertThrows(IllegalStateException.class, () -> config.jwtKey(props(secret)));
    }

    @Test
    void duzinaKljucaSeMeriUBajtovima() {
        var key = config.jwtKey(props("č".repeat(16)));
        assertEquals(32, key.getEncoded().length);
        assertEquals("HmacSHA256", key.getAlgorithm());
    }

    @Test
    void hs256TokenCuvaDogovoreniIdentitetIUlogu() {
        var key = config.jwtKey(props("test-only-jwt-secret-at-least-32-bytes"));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("student@test.rs")
                .issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("uloga", "STUDENT").claim("korisnikId", 10L).claim("studentId", 1L).build();
        String token = config.jwtEncoder(key).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        Jwt decoded = config.jwtDecoder(key).decode(token);
        assertEquals("student@test.rs", decoded.getSubject());
        assertEquals("STUDENT", decoded.getClaimAsString("uloga"));
        assertEquals(1L, ((Number) decoded.getClaim("studentId")).longValue());
    }

    @Test
    void lozinkePodrzavajuNoviFormatIPostojeceBcryptHashove() {
        var encoder = config.passwordEncoder();
        String noviHash = encoder.encode("lozinka");
        assertTrue(noviHash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches("lozinka", noviHash));
        String stariHash = new BCryptPasswordEncoder().encode("lozinka");
        assertTrue(encoder.matches("lozinka", stariHash));
        assertFalse(encoder.matches("pogresna", stariHash));
    }

    private SecurityProperties props(String secret) {
        return new SecurityProperties(secret, Duration.ofHours(1), List.of("http://localhost:3000"));
    }
}
