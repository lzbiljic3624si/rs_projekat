package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// HTTP ugovor i validacija; stvarni SecurityFilterChain se proverava u SecurityHttpTest.
class ControllerHttpTest {
    private final PredmetService predmeti = mock(PredmetService.class);
    private final PrijavaService prijave = mock(PrijavaService.class);
    private final RadService radovi = mock(RadService.class);
    private final RasporedService raspored = mock(RasporedService.class);
    private final ZadatakService zadaci = mock(ZadatakService.class);
    private final Authentication auth = new UsernamePasswordAuthenticationToken("student@test.rs", null);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PredmetController(predmeti), new PrijavaController(prijave), new RadController(radovi),
            new RasporedController(raspored), new ZadatakController(zadaci)).build();

    @Test
    void predmetDodavanjeICitanje() throws Exception {
        Predmet predmet = new Predmet();
        predmet.setId(1L);
        predmet.setNaziv("Razvoj softvera");
        predmet.setJezik("Java");
        predmet.setUslov("Nema");
        when(predmeti.save(any())).thenReturn(predmet);
        when(predmeti.findAll()).thenReturn(List.of(predmet));
        when(predmeti.findById(1L)).thenReturn(predmet);

        mvc.perform(post("/api/predmet/add").contentType("application/json").content("""
                {"naziv":"Razvoj softvera","jezik":"Java","uslov":"Nema"}
                """)).andExpect(status().isCreated()).andExpect(content().string("1"));
        verify(predmeti).save(argThat(p -> "Razvoj softvera".equals(p.getNaziv())
                && "Java".equals(p.getJezik()) && "Nema".equals(p.getUslov())));
        mvc.perform(get("/api/predmet/all")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].naziv").value("Razvoj softvera"));
        mvc.perform(get("/api/predmet/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.jezik").value("Java"))
                .andExpect(jsonPath("$.uslov").value("Nema"));
    }

    @Test
    void prijaveProsledjujuIdIIdentitetIMapirajuOdgovor() throws Exception {
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setId(8L);
        when(prijave.prijaviSe(3L, auth)).thenReturn(8L);
        when(prijave.moje(auth)).thenReturn(List.of(prijava));
        when(prijave.prijavaZaGrupu(3L, auth)).thenReturn(List.of(prijava));

        mvc.perform(post("/api/prijave/grupa/3").principal(auth))
                .andExpect(status().isCreated()).andExpect(content().string("8"));
        mvc.perform(get("/api/prijave/moje").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].status").doesNotExist());
        mvc.perform(get("/api/prijave/grupa/3").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].status").doesNotExist());
        verify(prijave).prijaviSe(3L, auth);
        verify(prijave).moje(auth);
        verify(prijave).prijavaZaGrupu(3L, auth);
    }

    @Test
    void odobravanjePrijaveViseNePostoji() throws Exception {
        mvc.perform(post("/api/prijave/8/prihvati").principal(auth))
                .andExpect(status().isNotFound());
        verifyNoInteractions(prijave);
    }

    @Test
    void radoviCitanjePreuzimanjePredajaIOcena() throws Exception {
        Rad rad = new Rad();
        rad.setId(9L);
        rad.setStatus("OCENJEN");
        rad.setPoeni(8.5);
        when(radovi.moj(auth)).thenReturn(List.of(rad));
        when(radovi.predati(4L, auth)).thenReturn(List.of(rad));
        when(radovi.preuzmi(5L, auth)).thenReturn(9L);
        when(radovi.predaj(eq(9L), any(), eq(auth))).thenReturn(rad);
        when(radovi.oceni(eq(9L), any(), eq(auth))).thenReturn(rad);

        mvc.perform(get("/api/rad/moj").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
        mvc.perform(get("/api/rad/predmet/4/predati").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poeni").value(8.5));
        mvc.perform(post("/api/rad/5/preuzmi").principal(auth))
                .andExpect(status().isCreated()).andExpect(content().string("9"));
        mvc.perform(post("/api/rad/9/predaj").principal(auth).contentType("application/json").content("""
                {"repoPutanjaResenja":"https://github.com/student/resenje"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9));
        mvc.perform(post("/api/rad/9/oceni").principal(auth).contentType("application/json").content("{\"poeni\":8.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OCENJEN"));
        verify(radovi).predati(4L, auth);
        verify(radovi).preuzmi(5L, auth);
        verify(radovi).predaj(eq(9L), argThat(r -> "https://github.com/student/resenje".equals(r.getRepoPutanjaResenja())), eq(auth));
        verify(radovi).oceni(eq(9L), argThat(r -> Double.valueOf(8.5).equals(r.getPoeni())), eq(auth));
    }

    @Test
    void rasporedCitaSveCetiriListe() throws Exception {
        Rok rok = new Rok(); rok.setId(6L); rok.setNaziv("Junski"); rok.setSkolskaGodina(2026);
        Ucionica ucionica = new Ucionica(); ucionica.setId(5L); ucionica.setNaziv("Lab 1");
        Termin termin = new Termin(); termin.setId(7L); termin.setRok(rok); termin.setUcionica(ucionica);
        termin.setVremePocetak(LocalTime.of(10, 0)); termin.setVremeKraj(LocalTime.of(12, 0));
        Grupa grupa = new Grupa(); grupa.setId(3L); grupa.setTermin(termin);
        when(raspored.rokovi()).thenReturn(List.of(rok));
        when(raspored.ucionice()).thenReturn(List.of(ucionica));
        when(raspored.termini(4L, 6L)).thenReturn(List.of(termin));
        when(raspored.grupe(7L)).thenReturn(List.of(grupa));

        mvc.perform(get("/api/raspored/rokovi")).andExpect(status().isOk()).andExpect(jsonPath("$[0].skolskaGodina").value(2026));
        mvc.perform(get("/api/raspored/ucionice")).andExpect(status().isOk()).andExpect(jsonPath("$[0].naziv").value("Lab 1"));
        mvc.perform(get("/api/raspored/termini").param("predmetId", "4").param("rokId", "6"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].vremePocetka").value("10:00:00"));
        mvc.perform(get("/api/raspored/grupe").param("terminId", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].termin.ucionica.id").value(5));
        verify(raspored).termini(4L, 6L);
        verify(raspored).grupe(7L);
    }

    @Test
    void rasporedDodavanjeProsledjujeRequestPolja() throws Exception {
        when(raspored.dodajRok(any(), eq(auth))).thenReturn(6L);
        when(raspored.dodajUcionicu(any(), eq(auth))).thenReturn(5L);
        when(raspored.dodajTermin(any(), eq(auth))).thenReturn(7L);
        when(raspored.dodajGrupu(any(), eq(auth))).thenReturn(3L);
        mvc.perform(post("/api/raspored/rokovi").principal(auth).contentType("application/json")
                        .content("{\"naziv\":\"Junski\",\"skolskaGodina\":2026}"))
                .andExpect(status().isCreated()).andExpect(content().string("6"));
        mvc.perform(post("/api/raspored/ucionice").principal(auth).contentType("application/json")
                        .content("{\"naziv\":\"Lab 1\"}"))
                .andExpect(status().isCreated()).andExpect(content().string("5"));
        LocalDate datum = LocalDate.now().plusDays(1);
        mvc.perform(post("/api/raspored/termini").principal(auth).contentType("application/json").content("""
                {"datum":"%s","vremePocetak":"10:00","vremeKraja":"12:00","predmetId":4,"rokId":6,"ucionicaId":5}
                """.formatted(datum))).andExpect(status().isCreated()).andExpect(content().string("7"));
        mvc.perform(post("/api/raspored/grupe").principal(auth).contentType("application/json")
                        .content("{\"naziv\":\"G1\",\"terminId\":7}"))
                .andExpect(status().isCreated()).andExpect(content().string("3"));
        verify(raspored).dodajRok(argThat(r -> "Junski".equals(r.getNaziv()) && Integer.valueOf(2026).equals(r.getSkolskaGodina())), eq(auth));
        verify(raspored).dodajUcionicu(argThat(r -> "Lab 1".equals(r.getNaziv())), eq(auth));
        verify(raspored).dodajTermin(argThat(r -> datum.equals(r.getDatum()) && LocalTime.of(10, 0).equals(r.getVremePocetak())
                && LocalTime.of(12, 0).equals(r.getVremeKraja()) && Long.valueOf(4).equals(r.getPredmetId())
                && Long.valueOf(6).equals(r.getRokId()) && Long.valueOf(5).equals(r.getUcionicaId())), eq(auth));
        verify(raspored).dodajGrupu(argThat(r -> "G1".equals(r.getNaziv()) && Long.valueOf(7).equals(r.getTerminId())), eq(auth));
    }

    @Test
    void zadaciVracajuJedanObjekatIKreirajuZadatak() throws Exception {
        ZadatakResponse zadatak = new ZadatakResponse(); zadatak.setId(9L); zadatak.setNaziv("Prvi zadatak");
        when(zadaci.zaGrupu(3L, auth)).thenReturn(zadatak);
        when(zadaci.dodaj(any(), eq(auth))).thenReturn(9L);

        mvc.perform(get("/api/zadaci/grupa/3").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9)).andExpect(jsonPath("$.naziv").value("Prvi zadatak"));
        mvc.perform(post("/api/zadaci").principal(auth).contentType("application/json").content("""
                {"naziv":"Prvi zadatak","grupaId":3,"repoPutanja":"https://github.com/test/zadatak"}
                """)).andExpect(status().isCreated()).andExpect(content().string("9"));
        verify(zadaci).zaGrupu(3L, auth);
        verify(zadaci).dodaj(argThat(r -> "Prvi zadatak".equals(r.getNaziv()) && Long.valueOf(3).equals(r.getGrupaId())
                && "https://github.com/test/zadatak".equals(r.getRepoPutanja())), eq(auth));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/predmet/add", "/api/rad/9/predaj", "/api/rad/9/oceni", "/api/zadaci",
            "/api/raspored/rokovi", "/api/raspored/ucionice", "/api/raspored/termini", "/api/raspored/grupe"})
    void praznoTeloNeDolaziDoServisa(String putanja) throws Exception {
        mvc.perform(post(putanja).principal(auth).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(predmeti, prijave, radovi, raspored, zadaci);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"poeni\":-1}", "{\"poeni\":\"tekst\"}", "neispravan-json"})
    void neispravnaOcenaVraca400(String telo) throws Exception {
        mvc.perform(post("/api/rad/9/oceni").principal(auth).contentType("application/json").content(telo))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(radovi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/predmet/neispravan", "/api/raspored/termini", "/api/raspored/grupe?terminId=tekst"})
    void neispravanIdINedostajuciParametriVraca400(String putanja) throws Exception {
        mvc.perform(get(putanja)).andExpect(status().isBadRequest());
        verifyNoInteractions(predmeti, raspored);
    }

    @Test
    void datumUProslostiSeOdbijaPreServisa() throws Exception {
        mvc.perform(post("/api/raspored/termini").principal(auth).contentType("application/json").content("""
                {"datum":"%s","vremePocetak":"10:00","vremeKraja":"12:00","predmetId":4,"rokId":6,"ucionicaId":5}
                """.formatted(LocalDate.now().minusDays(1)))).andExpect(status().isBadRequest());
        verifyNoInteractions(raspored);
    }

    @ParameterizedTest
    @CsvSource({"403,FORBIDDEN", "404,NOT_FOUND", "409,CONFLICT"})
    void statusServisaSePrenosiNaHttp(int kod, HttpStatus status) throws Exception {
        when(zadaci.zaGrupu(3L, auth)).thenThrow(new ResponseStatusException(status));
        mvc.perform(get("/api/zadaci/grupa/3").principal(auth)).andExpect(status().is(kod));
    }
}
