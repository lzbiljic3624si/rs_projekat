
package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.UnauthorizedException;
import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.controllers.request.PromenaLozinkeRequest;
import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.config.SecurityProperties;
import com.inzinjeri.rs_projekat.model.Korisnik;
import com.inzinjeri.rs_projekat.model.Uloga;
import com.inzinjeri.rs_projekat.repositories.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.nio.charset.StandardCharsets;

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

    @Transactional
    public void promeniLozinku(PromenaLozinkeRequest request, Authentication auth) {
        Korisnik korisnik = korisnikRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Nalog ne postoji"));
        String stara = request.getStaraLozinka();
        if (stara == null || stara.isBlank() || stara.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Stara lozinka mora imati najvise 72 UTF-8 bajta");
        }
        if (!passwordEncoder.matches(stara, korisnik.getPasswordHash())) {
            throw new UnauthorizedException("Pogresna stara lozinka");
        }
        String nova = request.getNovaLozinka();
        if (nova == null || nova.isBlank() || nova.length() < 8
                || nova.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Nova lozinka mora imati najmanje 8 znakova i najvise 72 UTF-8 bajta");
        }
        if ((korisnik.getUloga() == Uloga.STUDENT && korisnik.getStudent() == null)
                || (korisnik.getUloga() == Uloga.PROFESOR && korisnik.getProfesor() == null)) {
            throw new ConflictException("Nalog nema povezan profil");
        }
        String hash = passwordEncoder.encode(nova);
        korisnik.setPasswordHash(hash);
        if (korisnik.getUloga() == Uloga.STUDENT) {
            korisnik.getStudent().setPasswordHash(hash);
        } else if (korisnik.getUloga() == Uloga.PROFESOR) {
            korisnik.getProfesor().setPasswordHash(hash);
        }
        korisnikRepository.save(korisnik);
    }

    @Transactional(readOnly = true)
    public Prijava login(String email, String lozinka) {

        // Pronalazimo korisnika i proveravamo lozinku
        Korisnik korisnik = korisnikRepository
                .findByEmail(email.trim())
                .filter(k -> passwordEncoder.matches(lozinka, k.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Pogresan email ili lozinka"));

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
