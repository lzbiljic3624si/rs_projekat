
package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.config.SecurityProperties;
import com.inzinjeri.rs_projekat.model.Korisnik;
import com.inzinjeri.rs_projekat.model.Uloga;
import com.inzinjeri.rs_projekat.repositories.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String CLAIM_KORISNIK_ID = "korisnikId";
    public static final String CLAIM_STUDENT_ID = "studentId";
    public static final String CLAIM_PROFESOR_ID = "profesorId";

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;

    public record Prijava(String token, Instant istice, Uloga uloga) {
    }

    @Transactional(readOnly = true)
    public Prijava login(String email, String lozinka) {

        // Pronalazimo korisnika i proveravamo lozinku
        Korisnik korisnik = korisnikRepository
                .findByEmail(email.trim())
                .filter(k -> passwordEncoder.matches(lozinka, k.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Pogresan email ili lozinka"
                ));

        Instant sada = Instant.now();
        Instant istice = sada.plus(securityProperties.jwtTrajanje());

        // Podaci koji se upisuju u JWT token.
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("rs-projekat")
                .subject(korisnik.getEmail())
                .issuedAt(sada)
                .expiresAt(istice)
                .claim(SecurityConfig.CLAIM_ULOGA, korisnik.getUloga().name())
                .claim(CLAIM_KORISNIK_ID, korisnik.getId());

        // Dodajemo ID studenta ili profesora, u zavisnosti od uloge.
        if (korisnik.getUloga() == Uloga.STUDENT) {

            if (korisnik.getStudent() == null) {
                throw new IllegalStateException(
                        "Korisnik sa ulogom STUDENT nema povezanog studenta"
                );
            }

            claims.claim(CLAIM_STUDENT_ID, korisnik.getStudent().getId());

        } else if (korisnik.getUloga() == Uloga.PROFESOR) {

            if (korisnik.getProfesor() == null) {
                throw new IllegalStateException(
                        "Korisnik sa ulogom PROFESOR nema povezanog profesora"
                );
            }

            claims.claim(CLAIM_PROFESOR_ID, korisnik.getProfesor().getId());
        }

        // Potpisujemo i generišemo JWT token.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims.build())
        ).getTokenValue();

        return new Prijava(token, istice, korisnik.getUloga());
    }
}
