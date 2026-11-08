package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PredmetController.class, PredajePredmetController.class, ZadatakController.class,
        RadController.class, PrijavaController.class})
@Import({SecurityConfig.class, PredmetService.class, PredajePredmetService.class, ZadatakService.class,
        RadService.class, PrijavaService.class, PristupService.class})
class ModelCrudHttpTest {
    @MockitoBean GitHubService github;
    @MockitoBean RokRepository rokovi;
    @Autowired MockMvc mvc;
    @MockitoBean PredmetRepository predmeti;
    @MockitoBean PredajePredmetRepository veze;
    @MockitoBean ProfesorRepository profesori;
    @MockitoBean StudentRepository studenti;
    @MockitoBean RadRepository radovi;
    @MockitoBean ZadatakRepository zadaci;
    @MockitoBean PrijavljenStudentRepository prijave;
    @MockitoBean GrupaRepository grupe;
    @MockitoBean TerminRepository termini;
    @MockitoBean Clock clock;
    private Predmet predmet;
    private PredajePredmet veza;
    private Student student;
    private Grupa grupa;
    private Zadatak zadatak;
    private Rad rad;
    private PrijavljenStudent prijava;

    @BeforeEach
    void podaci() {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Belgrade")); vreme("09:00:00");
        predmet = new Predmet(); predmet.setId(4L); predmet.setNaziv("RS"); predmet.setJezik("Java");
        Profesor profesor = new Profesor(); profesor.setId(2L); profesor.setEmail("profesor@raf.rs");
        veza = new PredajePredmet(); veza.setId(9L); veza.setPredmet(predmet); veza.setProfesor(profesor);
        student = new Student(); student.setId(7L); student.setEmail("student@raf.rs");
        Termin termin = new Termin(); termin.setId(6L); termin.setPredmet(predmet);
        termin.setDatum(LocalDate.of(2030,6,15)); termin.setVremePocetak(LocalTime.of(10,0)); termin.setVremeKraj(LocalTime.of(12,0));
        grupa = new Grupa(); grupa.setId(3L); grupa.setTermin(termin);
        zadatak = new Zadatak(); zadatak.setId(5L); zadatak.setGrupa(grupa); zadatak.setNaziv("Z1");
        rad = new Rad(); rad.setId(8L); rad.setZadatak(zadatak); rad.setStudent(student); rad.setStatus(StatusRada.PREDAT);
        rad.setVremePredaje(LocalDateTime.of(2030,6,15,10,15)); rad.setRepoPutanjaResenja("staro");
        prijava = new PrijavljenStudent(); prijava.setId(10L); prijava.setStudent(student); prijava.setGrupa(grupa);
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet));
        when(predmeti.getPredmetByNaziv("RS")).thenReturn(List.of(predmet));
        when(predmeti.getPredmetByNaziv("Novi")).thenReturn(List.of(predmet));
        when(profesori.findByEmailIgnoreCase("profesor@raf.rs")).thenReturn(Optional.of(profesor));
        when(veze.findById(9L)).thenReturn(Optional.of(veza));
        when(veze.getPredmetiZaPredavaca("profesor@raf.rs")).thenReturn(List.of(veza));
        when(studenti.pronadjiPoEmailu("student@raf.rs")).thenReturn(Optional.of(student));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa)); when(grupe.findByIdZaIzmenu(3L)).thenReturn(Optional.of(grupa));
        when(zadaci.findById(5L)).thenReturn(Optional.of(zadatak)); when(zadaci.findByIdZaIzmenu(5L)).thenReturn(Optional.of(zadatak));
        when(radovi.findById(8L)).thenReturn(Optional.of(rad));
        when(radovi.findByIdZaIzmenu(8L)).thenReturn(Optional.of(rad));
        when(prijave.findById(10L)).thenReturn(Optional.of(prijava));
        when(predmeti.save(any())).thenAnswer(i -> i.getArgument(0));
        when(veze.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(zadaci.save(any())).thenAnswer(i -> i.getArgument(0));
        when(radovi.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prijave.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void predmetSeMenjaBezPromeneId() throws Exception {
        mvc.perform(put("/api/predmet/edit/4").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"Novi\",\"jezik\":\"Python\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.naziv").value("Novi"))
                .andExpect(jsonPath("$.jezik").value("Python"))
                .andExpect(jsonPath("$.length()").value(3));
        assertEquals("Python", predmet.getJezik());
    }

    @Test
    void dodelaPredmetaSeMenjaUzIstiId() throws Exception {
        mvc.perform(put("/api/predaje-predmet/edit/9").with(profesor()).contentType("application/json")
                .content("{\"nazivPredmeta\":\"RS\",\"emailProfesora\":\"profesor@raf.rs\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.profesor.passwordHash").doesNotExist());
    }

    @Test
    void zadatakImaJasnuEditRutu() throws Exception {
        mvc.perform(put("/api/zadaci/edit/5").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"Novi zadatak\",\"grupaId\":3,\"repoPutanja\":\"novi-repo\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.naziv").value("Novi zadatak"));
    }

    @ParameterizedTest
    @CsvSource({"/api/predmet/delete/4", "/api/predaje-predmet/delete/9", "/api/zadaci/delete/5", "/api/rad/delete/8", "/api/prijave/delete/10"})
    void profesorFizickiBrisePostojeciRed(String putanja) throws Exception {
        mvc.perform(delete(putanja).with(profesor())).andExpect(status().isNoContent()).andExpect(content().string(""));
        if (putanja.contains("predmet/delete")) {
            if (putanja.contains("predaje-predmet")) verify(veze).delete(veza); else verify(predmeti).delete(predmet);
        } else if (putanja.contains("zadaci")) verify(zadaci).delete(zadatak);
        else if (putanja.contains("rad/")) verify(radovi).delete(rad);
        else verify(prijave).delete(prijava);
    }

    @ParameterizedTest
    @CsvSource({"/api/predmet/delete/999", "/api/predaje-predmet/delete/999", "/api/zadaci/delete/999", "/api/rad/delete/999", "/api/prijave/delete/999"})
    void nepostojeciRedVraca404(String putanja) throws Exception {
        mvc.perform(delete(putanja).with(profesor())).andExpect(status().isNotFound());
        verify(predmeti, never()).delete(any()); verify(veze, never()).delete(any());
        verify(zadaci, never()).delete(any()); verify(radovi, never()).delete(any()); verify(prijave, never()).delete(any());
    }

    @ParameterizedTest
    @CsvSource({"/api/predmet/edit/999,predmet", "/api/predaje-predmet/edit/999,veza", "/api/zadaci/edit/999,zadatak",
            "/api/rad/edit/999,rad", "/api/prijave/edit/999,prijava"})
    void izmenaNepostojecegRedaVraca404(String putanja, String tip) throws Exception {
        String telo = switch(tip) {
            case "predmet" -> "{\"naziv\":\"RS\",\"jezik\":\"Java\"}";
            case "veza" -> "{\"nazivPredmeta\":\"RS\",\"emailProfesora\":\"profesor@raf.rs\"}";
            case "zadatak" -> "{\"naziv\":\"Z2\",\"grupaId\":3,\"repoPutanja\":\"repo\"}";
            case "rad" -> "{\"poeni\":10}";
            default -> "{\"grupaId\":3}";
        };
        mvc.perform(put(putanja).with(profesor()).contentType("application/json").content(telo))
                .andExpect(status().isNotFound());
    }

    @Test
    void predmetSaVezamaIliTerminimaSeNeBrise() throws Exception {
        when(veze.existsByPredmet_Id(4L)).thenReturn(true);
        mvc.perform(delete("/api/predmet/delete/4").with(profesor())).andExpect(status().isConflict());
        when(veze.existsByPredmet_Id(4L)).thenReturn(false); when(termini.existsByPredmet_Id(4L)).thenReturn(true);
        mvc.perform(delete("/api/predmet/delete/4").with(profesor())).andExpect(status().isConflict());
        verify(predmeti, never()).delete(any());
    }

    @Test
    void duplikatiPredmetaIDodeleNeMenjajuRed() throws Exception {
        Predmet drugi = new Predmet(); drugi.setId(99L);
        when(predmeti.getPredmetByNaziv("Zauzet")).thenReturn(List.of(drugi));
        mvc.perform(put("/api/predmet/edit/4").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"Zauzet\",\"jezik\":\"Java\"}")).andExpect(status().isConflict());
        when(veze.existsByProfesor_IdAndPredmet_IdAndIdNot(2L,4L,9L)).thenReturn(true);
        mvc.perform(put("/api/predaje-predmet/edit/9").with(profesor()).contentType("application/json")
                .content("{\"nazivPredmeta\":\"RS\",\"emailProfesora\":\"profesor@raf.rs\"}"))
                .andExpect(status().isConflict());
        assertEquals("RS", predmet.getNaziv()); verify(predmeti, never()).save(any());
        verify(veze, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"/api/predmet/edit/4", "/api/predaje-predmet/edit/9", "/api/zadaci/edit/5"})
    void praznaProfesorskaIzmenaJe400AStudentJe403(String putanja) throws Exception {
        mvc.perform(put(putanja).with(profesor()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(putanja).with(student()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(putanja).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"/api/predmet/delete/4", "/api/predaje-predmet/delete/9", "/api/zadaci/delete/5", "/api/rad/delete/8"})
    void studentNeBriseProfesorskePodatke(String putanja) throws Exception {
        mvc.perform(delete(putanja).with(student())).andExpect(status().isForbidden());
        mvc.perform(delete(putanja)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"/api/zadaci/delete/5", "/api/rad/delete/8", "/api/prijave/delete/10"})
    void tudjiProfesorNeBrisePodatkePredmeta(String putanja) throws Exception {
        mvc.perform(delete(putanja).with(jwt().jwt(j -> j.subject("tudji@raf.rs"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))).andExpect(status().isForbidden());
    }

    @Test
    void zadatakSaRadovimaSeNeBrise() throws Exception {
        when(radovi.existsByZadatak_Id(5L)).thenReturn(true);
        mvc.perform(delete("/api/zadaci/delete/5").with(profesor())).andExpect(status().isConflict());
        verify(zadaci, never()).delete(any());
    }

    @Test
    void studentNeMozeDaMenjaDodeljenuPutanjuPrekoEditRute() throws Exception {
        vreme("10:30:00");
        mvc.perform(put("/api/rad/edit/8").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"novi-link\",\"status\":\"OCENJEN\",\"studentId\":999}"))
                .andExpect(status().isForbidden());
        assertEquals("staro", rad.getRepoPutanjaResenja());
        assertEquals(StatusRada.PREDAT, rad.getStatus()); assertSame(student, rad.getStudent());
        assertEquals(LocalDateTime.of(2030,6,15,10,15), rad.getVremePredaje());
    }

    @Test
    void studentNeMenjaPoeneNitiOcenjenRad() throws Exception {
        vreme("10:30:00");
        mvc.perform(put("/api/rad/edit/8").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"novi-link\",\"poeni\":100}"))
                .andExpect(status().isForbidden());
        rad.setStatus(StatusRada.OCENJEN);
        mvc.perform(put("/api/rad/edit/8").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"novi-link\"}")).andExpect(status().isForbidden());
        assertEquals("staro", rad.getRepoPutanjaResenja()); assertNull(rad.getPoeni());
    }

    @ParameterizedTest
    @CsvSource({"09:59:59", "12:00:00"})
    void studentNeMenjaPutanjuNiVanTermina(String vreme) throws Exception {
        vreme(vreme);
        mvc.perform(put("/api/rad/edit/8").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"novi-link\"}")).andExpect(status().isForbidden());
        assertEquals("staro", rad.getRepoPutanjaResenja());
    }

    @Test
    void profesorIspravljaOcenuBezPromeneStatusa() throws Exception {
        rad.setStatus(StatusRada.OCENJEN); rad.setPoeni(50.0);
        mvc.perform(put("/api/rad/edit/8").with(profesor()).contentType("application/json")
                .content("{\"poeni\":85,\"repoPutanjaResenja\":\"tudja-putanja\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.poeni").value(85));
        assertEquals(StatusRada.OCENJEN, rad.getStatus()); assertEquals("staro", rad.getRepoPutanjaResenja());
    }

    @ParameterizedTest
    @CsvSource({"{}", "{\"poeni\":-1}", "{\"poeni\":\"NaN\"}"})
    void neispravnaIspravkaOceneNeMenjaRad(String telo) throws Exception {
        rad.setStatus(StatusRada.OCENJEN); rad.setPoeni(50.0);
        mvc.perform(put("/api/rad/edit/8").with(profesor()).contentType("application/json").content(telo))
                .andExpect(status().isBadRequest());
        assertEquals(50.0, rad.getPoeni());
    }

    @Test
    void profesorNeMenjaStudentskoResenjeINePreskaceOcenjivanje() throws Exception {
        mvc.perform(put("/api/rad/edit/8").with(profesor()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"tudje-resenje\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/rad/edit/8").with(profesor()).contentType("application/json")
                .content("{\"poeni\":85}")).andExpect(status().isConflict());
        assertEquals(StatusRada.PREDAT, rad.getStatus());
    }

    @Test
    void studentMenjaSamoGrupuSvojePrijave() throws Exception {
        Grupa nova = new Grupa(); nova.setId(11L); nova.setTermin(grupa.getTermin());
        when(grupe.findByIdZaIzmenu(11L)).thenReturn(Optional.of(nova));
        mvc.perform(put("/api/prijave/edit/10").with(student()).contentType("application/json")
                .content("{\"grupaId\":11,\"studentId\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grupa.id").value(11));
        assertSame(student, prijava.getStudent());
    }

    @Test
    void studentBriseSvojuPrijavuPrePocetka() throws Exception {
        mvc.perform(delete("/api/prijave/delete/10").with(student())).andExpect(status().isNoContent());
        verify(prijave).delete(prijava);
    }

    @Test
    void promenaPrijaveProveravaNovuGrupuIVremeIDuplikat() throws Exception {
        Grupa nova = new Grupa(); nova.setId(11L); nova.setTermin(grupa.getTermin());
        when(grupe.findByIdZaIzmenu(11L)).thenReturn(Optional.of(nova));
        when(prijave.existsByStudent_IdAndGrupa_Id(7L,11L)).thenReturn(true);
        mvc.perform(put("/api/prijave/edit/10").with(student()).contentType("application/json")
                .content("{\"grupaId\":11}")).andExpect(status().isConflict());
        when(prijave.existsByStudent_IdAndGrupa_Id(7L,11L)).thenReturn(false);
        Termin prosli = new Termin(); prosli.setPredmet(predmet); prosli.setDatum(LocalDate.of(2030,6,14));
        prosli.setVremePocetak(LocalTime.of(10,0)); prosli.setVremeKraj(LocalTime.of(12,0)); nova.setTermin(prosli);
        mvc.perform(put("/api/prijave/edit/10").with(student()).contentType("application/json")
                .content("{\"grupaId\":11}")).andExpect(status().isConflict());
        assertEquals(3L, prijava.getGrupa().getId()); verify(prijave, never()).saveAndFlush(any());
    }

    @Test
    void profesorMoraPredavatiPredmetINoveGrupe() throws Exception {
        Predmet tudji = new Predmet(); tudji.setId(99L);
        Termin termin = new Termin(); termin.setPredmet(tudji); termin.setDatum(LocalDate.of(2030,6,15));
        termin.setVremePocetak(LocalTime.of(10,0)); termin.setVremeKraj(LocalTime.of(12,0));
        Grupa nova = new Grupa(); nova.setId(11L); nova.setTermin(termin);
        when(grupe.findByIdZaIzmenu(11L)).thenReturn(Optional.of(nova));
        mvc.perform(put("/api/prijave/edit/10").with(profesor()).contentType("application/json")
                .content("{\"grupaId\":11}")).andExpect(status().isForbidden());
        assertEquals(3L, prijava.getGrupa().getId());
    }

    @Test
    void studentNeBriseTudjuPrijavuINeMenjaTudjiRad() throws Exception {
        var drugi = jwt().jwt(j -> j.subject("drugi@raf.rs")).authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
        mvc.perform(delete("/api/prijave/delete/10").with(drugi)).andExpect(status().isForbidden());
        mvc.perform(put("/api/rad/edit/8").with(drugi).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"novi\"}")).andExpect(status().isForbidden());
        assertEquals("staro", rad.getRepoPutanjaResenja()); verify(prijave, never()).delete(any());
    }

    @Test
    void prijavaSeNeMenjaPoslePocetkaIliPreuzimanja() throws Exception {
        vreme("10:00:00");
        mvc.perform(put("/api/prijave/edit/10").with(student()).contentType("application/json")
                .content("{\"grupaId\":3}")).andExpect(status().isConflict());
        vreme("09:00:00"); when(radovi.existsByStudent_IdAndZadatak_Grupa_Id(7L,3L)).thenReturn(true);
        mvc.perform(delete("/api/prijave/delete/10").with(student())).andExpect(status().isConflict());
        verify(prijave, never()).delete(any());
    }

    @Test
    void studentNeMenjaTudjuPrijavu() throws Exception {
        mvc.perform(put("/api/prijave/edit/10").with(jwt().jwt(j -> j.subject("drugi@raf.rs"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))).contentType("application/json")
                .content("{\"grupaId\":3}")).andExpect(status().isForbidden());
    }

    private void vreme(String time) { when(clock.instant()).thenReturn(LocalDateTime.parse("2030-06-15T"+time)
            .atZone(ZoneId.of("Europe/Belgrade")).toInstant()); }
    private JwtRequestPostProcessor profesor() { return jwt().jwt(j -> j.subject("profesor@raf.rs"))
            .authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")); }
    private JwtRequestPostProcessor student() { return jwt().jwt(j -> j.subject("student@raf.rs"))
            .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")); }
}
