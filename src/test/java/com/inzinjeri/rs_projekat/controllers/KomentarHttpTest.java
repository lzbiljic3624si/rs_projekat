package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.KomentarService;
import com.inzinjeri.rs_projekat.services.PristupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KomentarController.class)
@Import({SecurityConfig.class, KomentarService.class, PristupService.class})
class KomentarHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean KomentarRepository komentari;
    @MockitoBean RadRepository radovi;
    @MockitoBean PredajePredmetRepository predavanja;
    @MockitoBean TerminRepository termini;
    @MockitoBean GrupaRepository grupe;
    @MockitoBean StudentRepository studenti;
    @MockitoBean PrijavljenStudentRepository prijave;
    @MockitoBean ZadatakRepository zadaci;
    private Komentar komentar;

    @BeforeEach
    void podaci() {
        Predmet predmet = new Predmet(); predmet.setId(4L);
        Termin termin = new Termin(); termin.setPredmet(predmet);
        Grupa grupa = new Grupa(); grupa.setTermin(termin);
        Zadatak zadatak = new Zadatak(); zadatak.setGrupa(grupa);
        Student student = new Student(); student.setEmail("student@test.rs");
        Rad rad = new Rad(); rad.setId(9L); rad.setStudent(student); rad.setZadatak(zadatak);
        rad.setStatus(StatusRada.PREDAT);
        komentar = new Komentar(); komentar.setId(12L); komentar.setRad(rad);
        komentar.setKomentar("Proveri granicu petlje"); komentar.setPutanjaFajla("src/Main.java");
        komentar.setBrojLinije(17);
        PredajePredmet veza = new PredajePredmet(); veza.setPredmet(predmet);
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(komentari.findById(12L)).thenReturn(Optional.of(komentar));
        when(komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(9L)).thenReturn(List.of(komentar));
        when(komentari.save(any())).thenAnswer(i -> {
            Komentar novi = i.getArgument(0);
            if (novi.getId() == null) novi.setId(13L);
            return novi;
        });
        when(predavanja.getPredmetiZaPredavaca("profesor@test.rs")).thenReturn(List.of(veza));
    }

    @Test
    void vlasnikCitaPojedinacanKomentarSaLokacijomGreske() throws Exception {
        mvc.perform(get("/api/komentar/12").with(user("student@test.rs").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.radId").value(9))
                .andExpect(jsonPath("$.komentar").value("Proveri granicu petlje"))
                .andExpect(jsonPath("$.putanjaFajla").value("src/Main.java"))
                .andExpect(jsonPath("$.brojLinije").value(17))
                .andExpect(jsonPath("$.rad").doesNotExist());
    }

    @Test
    void profesorDodajeKomentarNaKonkretnuLinijuFajla() throws Exception {
        mvc.perform(post("/api/komentar/rad/9").with(user("profesor@test.rs").roles("PROFESOR"))
                        .contentType("application/json")
                        .content(json("  Proveri granicu  ", "  src/Helper.java  ", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.radId").value(9))
                .andExpect(jsonPath("$.komentar").value("Proveri granicu"))
                .andExpect(jsonPath("$.putanjaFajla").value("src/Helper.java"))
                .andExpect(jsonPath("$.brojLinije").value(3));
    }

    @Test
    void listaMapiraKomentareBezIzlaganjaCelogRada() throws Exception {
        mvc.perform(get("/api/komentar/rad/9").with(user("profesor@test.rs").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].radId").value(9))
                .andExpect(jsonPath("$[0].komentar").value("Proveri granicu petlje"))
                .andExpect(jsonPath("$[0].putanjaFajla").value("src/Main.java"))
                .andExpect(jsonPath("$[0].brojLinije").value(17))
                .andExpect(jsonPath("$[0].rad").doesNotExist());
    }

    @Test
    void profesorBriseKomentarUzPrazanOdgovor() throws Exception {
        mvc.perform(delete("/api/komentar/12").with(user("profesor@test.rs").roles("PROFESOR")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(komentari).delete(komentar);
    }

    @Test
    void profesorMenjaKomentarILokacijuUzIstiIdIRad() throws Exception {
        mvc.perform(put("/api/komentar/12").with(user("profesor@test.rs").roles("PROFESOR"))
                        .contentType("application/json").content(json("  Ispravljen opis  ", "  src/Helper.java  ", 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.radId").value(9))
                .andExpect(jsonPath("$.komentar").value("Ispravljen opis"))
                .andExpect(jsonPath("$.putanjaFajla").value("src/Helper.java"))
                .andExpect(jsonPath("$.brojLinije").value(5));
        verify(komentari).save(komentar);
    }

    @ParameterizedTest(name = "{0} {1} kao {2}/{3} vraca {4}")
    @MethodSource("pravaPristupa")
    void sveOperacijeProveravajuUloguIVezuSaRadom(String metoda, String putanja,
            String email, String uloga, int status) throws Exception {
        MockHttpServletRequestBuilder zahtev = switch (metoda) {
            case "POST" -> post(putanja).contentType("application/json").content(json("Greska", "Main.java", 1));
            case "PUT" -> put(putanja).contentType("application/json").content(json("Greska", "Main.java", 1));
            case "DELETE" -> delete(putanja);
            default -> get(putanja);
        };
        if (email != null) zahtev.with(user(email).roles(uloga));
        mvc.perform(zahtev).andExpect(status().is(status));
        if (status == 401 || status == 403) {
            verify(komentari, never()).save(any());
            verify(komentari, never()).delete(any(Komentar.class));
        }
    }

    static Stream<Arguments> pravaPristupa() {
        return Stream.of(
                new String[]{"GET", "/api/komentar/12"},
                new String[]{"GET", "/api/komentar/rad/9"},
                new String[]{"POST", "/api/komentar/rad/9"},
                new String[]{"PUT", "/api/komentar/12"},
                new String[]{"DELETE", "/api/komentar/12"}
        ).flatMap(endpoint -> {
            boolean citanje = endpoint[0].equals("GET");
            int uspeh = endpoint[0].equals("POST") ? 201 : endpoint[0].equals("DELETE") ? 204 : 200;
            return Stream.of(
                    Arguments.of(endpoint[0], endpoint[1], "profesor@test.rs", "PROFESOR", uspeh),
                    Arguments.of(endpoint[0], endpoint[1], "student@test.rs", "STUDENT", citanje ? 200 : 403),
                    Arguments.of(endpoint[0], endpoint[1], "drugi.profesor@test.rs", "PROFESOR", 403),
                    Arguments.of(endpoint[0], endpoint[1], "drugi.student@test.rs", "STUDENT", 403),
                    Arguments.of(endpoint[0], endpoint[1], "nepoznat@test.rs", "DRUGA", 403),
                    Arguments.of(endpoint[0], endpoint[1], null, null, 401));
        });
    }

    @ParameterizedTest
    @MethodSource("neispravniZahtevi")
    void validacijaZahtevaVraca400BezCuvanja(String body, String polje) throws Exception {
        for (var zahtev : List.of(post("/api/komentar/rad/9"), put("/api/komentar/12"))) {
            mvc.perform(zahtev.with(user("profesor@test.rs").roles("PROFESOR"))
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors." + polje).exists());
        }
        verify(komentari, never()).save(any());
    }

    static Stream<Arguments> neispravniZahtevi() {
        return Stream.of(
                Arguments.of("{}", "komentar"),
                Arguments.of("{\"putanjaFajla\":\"Main.java\",\"brojLinije\":1}", "komentar"),
                Arguments.of("{\"komentar\":\"Greska\",\"brojLinije\":1}", "putanjaFajla"),
                Arguments.of("{\"komentar\":\"Greska\",\"putanjaFajla\":\"Main.java\"}", "brojLinije"),
                Arguments.of(json("", "Main.java", 1), "komentar"),
                Arguments.of(json(" \t\n", "Main.java", 1), "komentar"),
                Arguments.of(json("x".repeat(2001), "Main.java", 1), "komentar"),
                Arguments.of(json("Greska", "", 1), "putanjaFajla"),
                Arguments.of(json("Greska", " \t\n", 1), "putanjaFajla"),
                Arguments.of(json("Greska", "p".repeat(501), 1), "putanjaFajla"),
                Arguments.of(json("Greska", "Main.java", 0), "brojLinije"),
                Arguments.of(json("Greska", "Main.java", -1), "brojLinije"),
                Arguments.of("{\"komentar\":null,\"putanjaFajla\":null,\"brojLinije\":null}", "brojLinije"));
    }

    @Test
    void maksimalneDuzineProlazeHttpValidaciju() throws Exception {
        mvc.perform(post("/api/komentar/rad/9").with(user("profesor@test.rs").roles("PROFESOR"))
                        .contentType("application/json").content(json("x".repeat(2000), "p".repeat(500), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.komentar").value("x".repeat(2000)))
                .andExpect(jsonPath("$.putanjaFajla").value("p".repeat(500)));
    }

    @Test
    void nepostojeciResursiVracaju404() throws Exception {
        mvc.perform(get("/api/komentar/999").with(user("profesor@test.rs").roles("PROFESOR")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Komentar ne postoji"));
        mvc.perform(delete("/api/komentar/999").with(user("profesor@test.rs").roles("PROFESOR")))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/komentar/999").with(user("profesor@test.rs").roles("PROFESOR"))
                        .contentType("application/json").content(json("Greska", "Main.java", 1)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/komentar/rad/999").with(user("profesor@test.rs").roles("PROFESOR")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/komentar/rad/999").with(user("profesor@test.rs").roles("PROFESOR"))
                        .contentType("application/json").content(json("Greska", "Main.java", 1)))
                .andExpect(status().isNotFound());
        verify(komentari, never()).save(any());
        verify(komentari, never()).delete(any(Komentar.class));
    }

    @Test
    void radBezKomentaraVracaPrazanNiz() throws Exception {
        when(komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(9L)).thenReturn(List.of());
        mvc.perform(get("/api/komentar/rad/9").with(user("student@test.rs").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/komentar/neispravan", "/api/komentar/rad/neispravan"})
    void neispravanIdVraca400(String putanja) throws Exception {
        mvc.perform(get(putanja).with(user("profesor@test.rs").roles("PROFESOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void neispravanJsonNeUpisujeKomentar() throws Exception {
        mvc.perform(post("/api/komentar/rad/9").with(user("profesor@test.rs").roles("PROFESOR"))
                        .contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        verify(komentari, never()).save(any());
    }

    private static String json(String tekst, String putanja, int linija) {
        return JsonMapper.builder().build().writeValueAsString(
                Map.of("komentar", tekst, "putanjaFajla", putanja, "brojLinije", linija));
    }
}
