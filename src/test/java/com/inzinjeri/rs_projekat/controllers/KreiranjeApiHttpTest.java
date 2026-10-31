package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({NalogController.class, PredajePredmetController.class, AuthController.class})
@Import({SecurityConfig.class, NalogService.class, PredajePredmetService.class, AuthService.class})
class KreiranjeApiHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder jwtEncoder;
    @MockitoBean private StudentRepository studenti;
    @MockitoBean private ProfesorRepository profesori;
    @MockitoBean private KorisnikRepository korisnici;
    @MockitoBean private PredmetRepository predmeti;
    @MockitoBean private PredajePredmetRepository veze;
    private final Map<String, Korisnik> nalozi = new HashMap<>();

    @BeforeEach
    void podaci() {
        when(studenti.saveAndFlush(any())).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(1L);
            return student;
        });
        when(profesori.saveAndFlush(any())).thenAnswer(invocation -> {
            Profesor profesor = invocation.getArgument(0);
            profesor.setId(2L);
            return profesor;
        });
        when(korisnici.saveAndFlush(any())).thenAnswer(invocation -> {
            Korisnik korisnik = invocation.getArgument(0);
            korisnik.setId((long) nalozi.size() + 1);
            nalozi.put(korisnik.getEmail(), korisnik);
            return korisnik;
        });
        when(korisnici.findByEmail(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(nalozi.get(invocation.getArgument(0))));
        when(korisnici.existsByEmailIgnoreCase(anyString())).thenAnswer(invocation ->
                nalozi.keySet().stream().anyMatch(email -> email.equalsIgnoreCase(invocation.getArgument(0))));
    }

    @Test
    void noviProfesorIStudentMoguOdmahDaSePrijave() throws Exception {
        mvc.perform(post("/api/nalozi/profesori").header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json").content("{\"ime\":\"Marija\",\"prezime\":\"Petrovic\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("mpetrovic@raf.rs"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        String odgovor = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"mpetrovic@raf.rs\",\"lozinka\":\"lozinka123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.uloga").value("PROFESOR"))
                .andReturn().getResponse().getContentAsString();
        String profesorToken = JsonPath.read(odgovor, "$.token");

        mvc.perform(post("/api/nalozi/studenti").header("Authorization", "Bearer " + profesorToken)
                        .contentType("application/json").content("""
                                {"ime":"Marko","prezime":"Nikolic","smer":"RN","brIndeksa":"12/2023","godUpisa":2023}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("mnikolic1223rn@raf.rs"))
                .andExpect(jsonPath("$.smer").value("RN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"mnikolic1223rn@raf.rs\",\"lozinka\":\"lozinka123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.uloga").value("STUDENT"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void noviProfesorDobijaSledeciSlobodanEmail() throws Exception {
        String authorization = "Bearer " + token("PROFESOR");
        mvc.perform(post("/api/nalozi/profesori").header("Authorization", authorization)
                        .contentType("application/json").content("{\"ime\":\"Marija\",\"prezime\":\"Petrovic\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("mpetrovic@raf.rs"));
        mvc.perform(post("/api/nalozi/profesori").header("Authorization", authorization)
                        .contentType("application/json").content("{\"ime\":\"Marina\",\"prezime\":\"Petrovic\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("mapetrovic@raf.rs"));
    }

    @Test
    void pogresnaLozinkaVracaProblemDetail401() throws Exception {
        mvc.perform(post("/api/nalozi/profesori").header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json").content("{\"ime\":\"Marija\",\"prezime\":\"Petrovic\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"mpetrovic@raf.rs\",\"lozinka\":\"pogresna-lozinka\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Pogresan email ili lozinka"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"/api/nalozi/studenti", "/api/nalozi/profesori", "/api/predaje-predmet"})
    void samoProfesorMozeDaKreiraPodatke(String putanja) throws Exception {
        mvc.perform(post(putanja).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(putanja).header("Authorization", "Bearer " + token("STUDENT"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(studenti, profesori, korisnici, predmeti, veze);
    }

    @ParameterizedTest
    @CsvSource({"/api/nalozi/studenti", "/api/nalozi/profesori", "/api/predaje-predmet"})
    void nepotpunZahtevNeUpisujePodatke(String putanja) throws Exception {
        mvc.perform(post(putanja).header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(studenti, profesori, korisnici, predmeti, veze);
    }

    @ParameterizedTest
    @CsvSource({"studenti,ime", "studenti,prezime", "profesori,ime", "profesori,prezime"})
    void predugackoImeIliPrezimeSeOdbijaPreUpisa(String tip, String polje) throws Exception {
        String ime = "ime".equals(polje) ? "A".repeat(256) : "Ana";
        String prezime = "prezime".equals(polje) ? "A".repeat(256) : "Ilic";
        mvc.perform(post("/api/nalozi/" + tip).header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json").content("""
                                {"ime":"%s","prezime":"%s","smer":"RN","brIndeksa":"12/2023","godUpisa":2023}
                                """.formatted(ime, prezime)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(studenti, profesori, korisnici);
    }

    @ParameterizedTest
    @CsvSource({"studenti", "profesori"})
    void predugacakGenerisaniEmailSeOdbijaPreUpisa(String tip) throws Exception {
        mvc.perform(post("/api/nalozi/" + tip).header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json").content("""
                                {"ime":"Ana","prezime":"%s","smer":"RN","brIndeksa":"12/2023","godUpisa":2023}
                                """.formatted("a".repeat(250))))
                .andExpect(status().isBadRequest());
        verify(studenti, never()).saveAndFlush(any());
        verify(profesori, never()).saveAndFlush(any());
        verify(korisnici, never()).saveAndFlush(any());
    }

    @Test
    void profesorMozeDaDodeliDrugogProfesoraPredmetuBezIzlaganjaHasha() throws Exception {
        Profesor profesor = new Profesor();
        profesor.setId(2L);
        profesor.setEmail("mapetrovic@raf.rs");
        profesor.setPasswordHash("tajni-hash");
        Predmet predmet = new Predmet();
        predmet.setId(3L);
        predmet.setNaziv("Razvoj softvera");
        when(profesori.findByEmailIgnoreCase("mapetrovic@raf.rs")).thenReturn(Optional.of(profesor));
        when(predmeti.getPredmetByNaziv("Razvoj softvera")).thenReturn(List.of(predmet));
        when(veze.saveAndFlush(any())).thenAnswer(invocation -> {
            PredajePredmet veza = invocation.getArgument(0);
            veza.setId(4L);
            return veza;
        });

        mvc.perform(post("/api/predaje-predmet").header("Authorization", "Bearer " + token("PROFESOR"))
                        .contentType("application/json").content("""
                                {"emailProfesora":"mapetrovic@raf.rs","nazivPredmeta":"Razvoj softvera"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.profesor.email").value("mapetrovic@raf.rs"))
                .andExpect(jsonPath("$.predmet.id").value(3))
                .andExpect(jsonPath("$.profesor.passwordHash").doesNotExist());
    }

    private String token(String uloga) {
        Instant sada = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("ocenjivac@raf.rs")
                .issuedAt(sada).expiresAt(sada.plusSeconds(3600)).claim("uloga", uloga).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
