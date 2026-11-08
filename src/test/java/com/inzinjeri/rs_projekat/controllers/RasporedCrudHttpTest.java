package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({SecurityConfig.class, RasporedService.class})
class RasporedCrudHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @MockitoBean Clock clock;
    @MockitoBean KorisnikRepository korisnici;
    @MockitoBean RokRepository rokovi;
    @MockitoBean UcionicaRepository ucionice;
    @MockitoBean TerminRepository termini;
    @MockitoBean GrupaRepository grupe;
    @MockitoBean PredmetRepository predmeti;
    @MockitoBean PredajePredmetRepository predaje;
    @MockitoBean RadRepository radovi;
    @MockitoBean PrijavljenStudentRepository prijave;
    @MockitoBean ZadatakRepository zadaci;
    @MockitoBean AuthService authService;
    @MockitoBean NalogService nalogService;
    @MockitoBean PredmetService predmetService;
    @MockitoBean PredajePredmetService predajePredmetService;
    @MockitoBean PrijavaService prijavaService;
    @MockitoBean RadService radService;
    @MockitoBean ZadatakService zadatakService;
    @MockitoBean KomentarService komentarService;
    @MockitoBean(name = "pristupService") PristupService pristupService;

    private Rok rok;
    private Ucionica ucionica;
    private Termin termin;
    private Grupa grupa;
    private final String rokJson = "{\"naziv\":\"Junski\",\"skolskaGodina\":2026}";
    private final String ucionicaJson = "{\"naziv\":\"Lab\"}";
    private final String terminJson = "{\"datum\":\"2099-06-01\",\"vremePocetak\":\"10:00\",\"vremeKraja\":\"12:00\",\"predmetId\":4,\"rokId\":6,\"ucionicaId\":5}";
    private final String grupaJson = "{\"naziv\":\"G1\",\"terminId\":7}";

    @BeforeEach
    void fixture() {
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Belgrade"));
        Korisnik profesor = new Korisnik(); profesor.setId(1L); profesor.setEmail("profesor@test.rs");
        profesor.setUloga(Uloga.PROFESOR);
        Korisnik student = new Korisnik(); student.setId(2L); student.setEmail("student@test.rs");
        student.setUloga(Uloga.STUDENT);
        when(korisnici.findById(1L)).thenReturn(Optional.of(profesor));
        when(korisnici.findById(2L)).thenReturn(Optional.of(student));
        rok = new Rok(); rok.setId(6L); rok.setNaziv("Junski"); rok.setSkolskaGodina(2026);
        ucionica = new Ucionica(); ucionica.setId(5L); ucionica.setNaziv("Lab");
        Predmet predmet = new Predmet(); predmet.setId(4L);
        termin = new Termin(); termin.setId(7L); termin.setPredmet(predmet); termin.setRok(rok);
        termin.setUcionica(ucionica); termin.setDatum(LocalDate.of(2099, 6, 1));
        termin.setVremePocetak(LocalTime.of(10, 0)); termin.setVremeKraj(LocalTime.of(12, 0));
        grupa = new Grupa(); grupa.setId(3L); grupa.setNaziv("G1"); grupa.setTermin(termin);
        when(rokovi.findById(6L)).thenReturn(Optional.of(rok));
        when(ucionice.findById(5L)).thenReturn(Optional.of(ucionica));
        when(termini.findById(7L)).thenReturn(Optional.of(termin));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(grupe.findByIdZaIzmenu(3L)).thenReturn(Optional.of(grupa));
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet));
        PredajePredmet veza = new PredajePredmet(); veza.setPredmet(predmet);
        when(predaje.getPredmetiZaPredavaca("profesor@test.rs")).thenReturn(List.of(veza));
        when(rokovi.save(any())).thenAnswer(call -> { Rok r = call.getArgument(0); r.setId(6L); return r; });
        when(ucionice.save(any())).thenAnswer(call -> { Ucionica u = call.getArgument(0); u.setId(5L); return u; });
        when(termini.save(any())).thenAnswer(call -> { Termin t = call.getArgument(0); t.setId(7L); return t; });
        when(grupe.save(any())).thenAnswer(call -> { Grupa g = call.getArgument(0); g.setId(3L); return g; });
        when(pristupService.profesorPredajePredmet(any(), anyLong())).thenReturn(true);
        when(pristupService.profesorPredajePredmetTermina(any(), anyLong())).thenReturn(true);
        when(pristupService.profesorPredajePredmetGrupe(any(), anyLong())).thenReturn(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/raspored"})
    void citaSveListeIPojedinacneZapise(String prefix) throws Exception {
        when(rokovi.findAll()).thenReturn(List.of(rok));
        when(ucionice.findAll()).thenReturn(List.of(ucionica));
        when(termini.pronadjiPoPredmetuIRoku(4L, 6L)).thenReturn(List.of(termin));
        when(grupe.getGrupeZaTermin(7L)).thenReturn(List.of(grupa));
        for (String path : List.of("/rokovi", "/ucionice", "/termini?predmetId=4&rokId=6", "/grupe?terminId=7")) {
            if (prefix.equals("/api/raspored") && path.startsWith("/termini")) continue;
            mvc.perform(get(prefix + path).header("Authorization", student())).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").isNumber());
        }
        for (String path : List.of("/rokovi/6", "/ucionice/5", "/termini/7", "/grupe/3")) {
            if (prefix.equals("/api/raspored") && path.startsWith("/termini")) continue;
            mvc.perform(get(prefix + path).header("Authorization", student())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/raspored"})
    void profesorKreiraMenjaIBrise(String prefix) throws Exception {
        String[] resources = {"rokovi", "ucionice", "termini", "grupe"};
        String[] bodies = {rokJson, ucionicaJson, terminJson, grupaJson};
        int[] ids = {6, 5, 7, 3};
        for (int i = 0; i < resources.length; i++) {
            if (prefix.equals("/api/raspored") && resources[i].equals("termini")) continue;
            mvc.perform(post(prefix + "/" + resources[i]).header("Authorization", profesor())
                            .contentType("application/json").content(bodies[i]))
                    .andExpect(status().isCreated()).andExpect(content().string(Integer.toString(ids[i])));
            mvc.perform(put(prefix + "/" + resources[i] + "/edit/" + ids[i]).header("Authorization", profesor())
                            .contentType("application/json").content(bodies[i]))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ids[i]));
            mvc.perform(delete(prefix + "/" + resources[i] + "/delete/" + ids[i]).header("Authorization", profesor()))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
        }
        verify(rokovi).delete(rok); verify(ucionice).delete(ucionica);
        if (prefix.equals("/api")) verify(termini).delete(termin);
        verify(grupe).delete(grupa);
    }

    @Test
    void zadrzavaStaruPutanjuGrupe() throws Exception {
        mvc.perform(put("/api/raspored/grupe/3").header("Authorization", profesor())
                        .contentType("application/json").content(grupaJson)).andExpect(status().isOk());
    }

    @Test
    void terminiNemajuDupluPutanju() throws Exception {
        mvc.perform(get("/api/raspored/termini").header("Authorization", student())
                .param("predmetId", "4").param("rokId", "6")).andExpect(status().isNotFound());
    }

    @Test
    void neOtvaraNovePutPutanje() throws Exception {
        mvc.perform(put("/api/termini/7").header("Authorization", profesor())
                .contentType("application/json").content(terminJson)).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/grupe/3").header("Authorization", profesor())
                .contentType("application/json").content(grupaJson)).andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rokovi", "ucionice", "termini", "grupe"})
    void bezPrijave401StudentNeMozeDaBrise(String resource) throws Exception {
        mvc.perform(get("/api/" + resource + "/99")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/" + resource + "/delete/99").header("Authorization", student()))
                .andExpect(status().isForbidden());
        verify(rokovi, never()).delete(any()); verify(ucionice, never()).delete(any());
        verify(termini, never()).delete(any()); verify(grupe, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/raspored"})
    void nepostojeciZapisi404(String prefix) throws Exception {
        String[] resources = {"rokovi", "ucionice", "termini", "grupe"};
        String[] bodies = {rokJson, ucionicaJson, terminJson, grupaJson};
        for (int i = 0; i < resources.length; i++) {
            String resource = resources[i];
            if (prefix.equals("/api/raspored") && resource.equals("termini")) continue;
            mvc.perform(get(prefix + "/" + resource + "/99").header("Authorization", student()))
                    .andExpect(status().isNotFound());
            mvc.perform(delete(prefix + "/" + resource + "/delete/99").header("Authorization", profesor()))
                    .andExpect(status().isNotFound());
            mvc.perform(put(prefix + "/" + resource + "/edit/99").header("Authorization", profesor())
                            .contentType("application/json").content(bodies[i]))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void studentNeKreiraIMenjaRaspored() throws Exception {
        String[] resources = {"rokovi", "ucionice", "termini", "grupe"};
        String[] bodies = {rokJson, ucionicaJson, terminJson, grupaJson};
        for (int i = 0; i < resources.length; i++) {
            mvc.perform(post("/api/" + resources[i]).header("Authorization", student())
                            .contentType("application/json").content(bodies[i])).andExpect(status().isForbidden());
            mvc.perform(put("/api/" + resources[i] + "/edit/99").header("Authorization", student())
                            .contentType("application/json").content(bodies[i])).andExpect(status().isForbidden());
        }
        verify(rokovi, never()).save(any()); verify(ucionice, never()).save(any());
        verify(termini, never()).save(any()); verify(grupe, never()).save(any());
    }

    @Test
    void obavezniFilteriIPolja400() throws Exception {
        for (String path : List.of("/api/termini", "/api/termini?predmetId=4", "/api/grupe")) {
            mvc.perform(get(path).header("Authorization", student())).andExpect(status().isBadRequest());
        }
        for (String resource : List.of("rokovi", "ucionice", "termini", "grupe")) {
            mvc.perform(post("/api/" + resource).header("Authorization", profesor())
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void profesorDrugogPredmetaNeMenjaTerminNiGrupu() throws Exception {
        when(predaje.getPredmetiZaPredavaca("profesor@test.rs")).thenReturn(List.of());
        mvc.perform(put("/api/termini/edit/7").header("Authorization", profesor())
                        .contentType("application/json").content(terminJson)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/grupe/delete/3").header("Authorization", profesor())).andExpect(status().isForbidden());
        verify(termini, never()).save(any()); verify(grupe, never()).delete(any());
    }

    @Test
    void sopstveniNazivNijeDuplikat() throws Exception {
        when(rokovi.existsByNazivAndSkolskaGodina("Junski", 2026)).thenReturn(true);
        when(ucionice.pronadjiPoNazivu("Lab")).thenReturn(Optional.of(ucionica));
        mvc.perform(put("/api/rokovi/edit/6").header("Authorization", profesor())
                        .contentType("application/json").content(rokJson)).andExpect(status().isOk());
        mvc.perform(put("/api/ucionice/edit/5").header("Authorization", profesor())
                        .contentType("application/json").content(ucionicaJson)).andExpect(status().isOk());
    }

    @Test
    void tudjiDuplikati409BezIzmena() throws Exception {
        when(rokovi.existsByNazivAndSkolskaGodina("Jul", 2026)).thenReturn(true);
        Ucionica druga = new Ucionica(); druga.setId(8L);
        when(ucionice.pronadjiPoNazivu("Druga")).thenReturn(Optional.of(druga));
        mvc.perform(put("/api/rokovi/edit/6").header("Authorization", profesor())
                        .contentType("application/json").content(rokJson.replace("Junski", "Jul")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/ucionice/edit/5").header("Authorization", profesor())
                        .contentType("application/json").content(ucionicaJson.replace("Lab", "Druga")))
                .andExpect(status().isConflict());
        assertEquals("Junski", rok.getNaziv()); assertEquals("Lab", ucionica.getNaziv());
        verify(rokovi, never()).save(any()); verify(ucionice, never()).save(any());
    }

    @Test
    void grupaSaPrijavamaZadatkomIliRadom409() throws Exception {
        when(prijave.existsByGrupa_Id(3L)).thenReturn(true);
        odbijBrisanjeGrupe();
        when(prijave.existsByGrupa_Id(3L)).thenReturn(false);
        when(zadaci.existsByGrupa_Id(3L)).thenReturn(true);
        odbijBrisanjeGrupe();
        when(zadaci.existsByGrupa_Id(3L)).thenReturn(false);
        when(radovi.existsByZadatak_Grupa_Id(3L)).thenReturn(true);
        odbijBrisanjeGrupe();
        verify(grupe, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/raspored"})
    void rokUcionicaITerminSaVezama409BezBrisanja(String prefix) throws Exception {
        when(termini.existsByRok_Id(6L)).thenReturn(true);
        when(termini.existsByUcionica_Id(5L)).thenReturn(true);
        when(grupe.existsByTermin_Id(7L)).thenReturn(true);
        for (String path : List.of("/rokovi/delete/6", "/ucionice/delete/5", "/termini/delete/7")) {
            if (prefix.equals("/api/raspored") && path.startsWith("/termini")) continue;
            mvc.perform(delete(prefix + path).header("Authorization", profesor())).andExpect(status().isConflict());
        }
        verify(rokovi, never()).delete(any()); verify(ucionice, never()).delete(any());
        verify(termini, never()).delete(any());
    }

    private void odbijBrisanjeGrupe() throws Exception {
        mvc.perform(delete("/api/grupe/delete/3").header("Authorization", profesor()))
                .andExpect(status().isConflict());
    }

    private String profesor() { return token("PROFESOR", "profesor@test.rs"); }
    private String student() { return token("STUDENT", "student@test.rs"); }
    private String token(String uloga, String email) {
        var claims = JwtClaimsSet.builder().subject(email).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600)).claim("uloga", uloga)
                .claim("korisnikId", "PROFESOR".equals(uloga) ? 1L : 2L).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

}
