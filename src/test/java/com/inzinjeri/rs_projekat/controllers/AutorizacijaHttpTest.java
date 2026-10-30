package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PredmetController.class, PrijavaController.class, RadController.class,
        RasporedController.class, ZadatakController.class})
@Import({SecurityConfig.class, PristupService.class})
class AutorizacijaHttpTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PredmetService predmeti;
    @MockitoBean private PrijavaService prijave;
    @MockitoBean private RadService radovi;
    @MockitoBean private RasporedService raspored;
    @MockitoBean private ZadatakService zadaci;
    @MockitoBean private PredajePredmetRepository predaje;
    @MockitoBean private TerminRepository termini;
    @MockitoBean private GrupaRepository grupe;
    @MockitoBean private RadRepository radRepository;
    @MockitoBean private StudentRepository studenti;
    @MockitoBean private PrijavljenStudentRepository prijavljeni;
    @MockitoBean private ZadatakRepository zadatakRepository;

    @BeforeEach
    void podaci() {
        Predmet predmet = new Predmet(); predmet.setId(4L);
        Termin termin = new Termin(); termin.setId(7L); termin.setPredmet(predmet);
        Grupa grupa = new Grupa(); grupa.setId(3L); grupa.setTermin(termin);
        Zadatak zadatak = new Zadatak(); zadatak.setId(5L); zadatak.setGrupa(grupa);
        Student student = new Student(); student.setId(1L); student.setEmail("student@test.rs");
        Rad rad = new Rad(); rad.setId(9L); rad.setStudent(student); rad.setZadatak(zadatak);
        PredajePredmet veza = new PredajePredmet(); veza.setPredmet(predmet);
        when(predaje.getPredmetiZaPredavaca("profesor@test.rs")).thenReturn(List.of(veza));
        when(termini.findById(7L)).thenReturn(Optional.of(termin));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(zadatakRepository.findById(5L)).thenReturn(Optional.of(zadatak));
        when(radRepository.findById(9L)).thenReturn(Optional.of(rad));
        when(studenti.pronadjiPoEmailu("student@test.rs")).thenReturn(Optional.of(student));
        when(prijavljeni.existsByStudent_IdAndGrupa_Id(1L, 3L)).thenReturn(true);
        when(predmeti.findById(4L)).thenReturn(predmet);
        when(predmeti.save(any())).thenReturn(predmet);
        when(radovi.predaj(any(), any(), any())).thenReturn(rad);
        when(radovi.oceni(any(), any(), any())).thenReturn(rad);
        when(zadaci.zaGrupu(any(), any())).thenReturn(new ZadatakResponse());
    }

    @ParameterizedTest(name = "{0} kao {1} {2} vraca {3}")
    @MethodSource("pravaPristupa")
    void endpointProveravaUloguIVlasnistvo(Endpoint endpoint, String email, String uloga, int kod) throws Exception {
        MockHttpServletRequestBuilder zahtev = endpoint.body() == null ? get(endpoint.path())
                : post(endpoint.path()).contentType("application/json").content(endpoint.body()).with(csrf());
        mvc.perform(zahtev.with(user(email).roles(uloga))).andExpect(status().is(kod));
        if (kod == 403) {
            verifyNoInteractions(predmeti, prijave, radovi, raspored, zadaci);
        }
    }

    @Test
    void nepoznataUlogaNeMozeDaCitaPodatke() throws Exception {
        mvc.perform(get("/api/predmet/all").with(user("nepoznat@test.rs").roles("DRUGA")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(predmeti);
    }

    @Test
    void anonimniKorisnikNeDobijaZadatak() throws Exception {
        mvc.perform(get("/api/zadaci/grupa/3").with(anonymous()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(zadaci);
    }

    @Test
    void nepostojeciRadSeOdbijaPrePoslovnogServisa() throws Exception {
        mvc.perform(post("/api/rad/999/predaj").with(user("student@test.rs").roles("STUDENT"))
                        .with(csrf()).contentType("application/json")
                        .content("{\"repoPutanjaResenja\":\"https://github.com/test/resenje\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(radovi);
    }

    static Stream<Arguments> pravaPristupa() {
        String termin = """
                {"datum":"%s","vremePocetak":"10:00","vremeKraja":"12:00","predmetId":4,"rokId":6,"ucionicaId":5}
                """.formatted(LocalDate.now().plusDays(1));
        return Stream.of(
                new Endpoint("/api/predmet/all", null, 200, 200, 200, 200),
                new Endpoint("/api/predmet/4", null, 200, 200, 200, 200),
                new Endpoint("/api/predmet/add", "{\"naziv\":\"RS\",\"jezik\":\"Java\",\"uslov\":\"OOP\"}", 403, 201, 403, 201),
                new Endpoint("/api/prijave/grupa/3", "", 201, 403, 201, 403),
                new Endpoint("/api/prijave/moje", null, 200, 403, 200, 403),
                new Endpoint("/api/prijave/grupa/3", null, 403, 200, 403, 403),
                new Endpoint("/api/rad/moj", null, 200, 403, 200, 403),
                new Endpoint("/api/rad/predmet/4/predati", null, 403, 200, 403, 403),
                new Endpoint("/api/rad/9/oceni", "{\"poeni\":8.5}", 403, 200, 403, 403),
                new Endpoint("/api/rad/9/predaj", "{\"repoPutanjaResenja\":\"https://github.com/test/resenje\"}", 200, 403, 403, 403),
                new Endpoint("/api/rad/5/preuzmi", "", 201, 403, 403, 403),
                new Endpoint("/api/raspored/rokovi", null, 200, 200, 200, 200),
                new Endpoint("/api/raspored/ucionice", null, 200, 200, 200, 200),
                new Endpoint("/api/raspored/termini?predmetId=4&rokId=6", null, 200, 200, 200, 200),
                new Endpoint("/api/raspored/grupe?terminId=7", null, 200, 200, 200, 200),
                new Endpoint("/api/raspored/rokovi", "{\"naziv\":\"Jun\",\"skolskaGodina\":2026}", 403, 201, 403, 201),
                new Endpoint("/api/raspored/ucionice", "{\"naziv\":\"Lab\"}", 403, 201, 403, 201),
                new Endpoint("/api/raspored/termini", termin, 403, 201, 403, 403),
                new Endpoint("/api/raspored/grupe", "{\"naziv\":\"G1\",\"terminId\":7}", 403, 201, 403, 403),
                new Endpoint("/api/zadaci/grupa/3", null, 200, 200, 403, 403),
                new Endpoint("/api/zadaci", "{\"naziv\":\"Z1\",\"grupaId\":3,\"repoPutanja\":\"https://github.com/test/zadatak\"}", 403, 201, 403, 403)
        ).flatMap(e -> Stream.of(
                Arguments.of(e, "student@test.rs", "STUDENT", e.student()),
                Arguments.of(e, "profesor@test.rs", "PROFESOR", e.profesor()),
                Arguments.of(e, "drugi.student@test.rs", "STUDENT", e.drugiStudent()),
                Arguments.of(e, "drugi.profesor@test.rs", "PROFESOR", e.drugiProfesor())));
    }

    record Endpoint(String path, String body, int student, int profesor, int drugiStudent, int drugiProfesor) {
        @Override
        public String toString() {
            return (body == null ? "GET " : "POST ") + path;
        }
    }
}
