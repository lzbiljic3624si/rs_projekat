package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

@WebMvcTest({RadController.class, PrijavaController.class, TerminController.class, GrupaController.class,
        ZadatakController.class, PredajePredmetController.class, AuthController.class})
@Import({SecurityConfig.class, RadService.class, PrijavaService.class, RasporedService.class,
        ZadatakService.class, PredajePredmetService.class, AuthService.class, PristupService.class})
class DopuneApiHttpTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean RadRepository radovi;
    @MockitoBean StudentRepository studenti;
    @MockitoBean ProfesorRepository profesori;
    @MockitoBean KorisnikRepository korisnici;
    @MockitoBean ZadatakRepository zadaci;
    @MockitoBean GrupaRepository grupe;
    @MockitoBean TerminRepository termini;
    @MockitoBean RokRepository rokovi;
    @MockitoBean UcionicaRepository ucionice;
    @MockitoBean PredmetRepository predmeti;
    @MockitoBean PredajePredmetRepository veze;
    @MockitoBean PrijavljenStudentRepository prijave;
    @MockitoBean Clock clock;
    private Termin termin;
    private Grupa grupa;
    private Zadatak zadatak;
    private Rad rad;
    private Student student;

    @BeforeEach
    void podaci() {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Belgrade"));
        vreme("09:00:00");
        Predmet predmet = new Predmet(); predmet.setId(4L); predmet.setNaziv("RS");
        Rok rok = new Rok(); rok.setId(1L);
        Ucionica ucionica = new Ucionica(); ucionica.setId(2L);
        termin = new Termin(); termin.setId(6L); termin.setPredmet(predmet); termin.setRok(rok);
        termin.setUcionica(ucionica); termin.setDatum(LocalDate.of(2030, 6, 15));
        termin.setVremePocetak(LocalTime.of(10, 0)); termin.setVremeKraj(LocalTime.of(12, 0));
        grupa = new Grupa(); grupa.setId(3L); grupa.setNaziv("G1"); grupa.setTermin(termin);
        zadatak = new Zadatak(); zadatak.setId(5L); zadatak.setGrupa(grupa);
        zadatak.setNaziv("Z1"); zadatak.setRepoPutanja("https://github.com/test/z1");
        student = new Student(); student.setId(7L); student.setEmail("student@raf.rs");
        rad = new Rad(); rad.setId(8L); rad.setStudent(student); rad.setZadatak(zadatak);
        rad.setStatus(StatusRada.PREUZET);
        PredajePredmet veza = new PredajePredmet(); veza.setId(9L); veza.setPredmet(predmet);
        when(veze.getPredmetiZaPredavaca("profesor@raf.rs")).thenReturn(List.of(veza));
        when(studenti.pronadjiPoEmailu("student@raf.rs")).thenReturn(Optional.of(student));
        when(termini.findById(6L)).thenReturn(Optional.of(termin));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(grupe.findByIdZaIzmenu(3L)).thenReturn(Optional.of(grupa));
        when(zadaci.findById(5L)).thenReturn(Optional.of(zadatak));
        when(zadaci.findByIdZaIzmenu(5L)).thenReturn(Optional.of(zadatak));
        when(radovi.findById(8L)).thenReturn(Optional.of(rad));
        when(radovi.findByIdZaIzmenu(8L)).thenReturn(Optional.of(rad));
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet));
        when(rokovi.findById(1L)).thenReturn(Optional.of(rok));
        when(ucionice.findById(2L)).thenReturn(Optional.of(ucionica));
        when(radovi.save(any())).thenAnswer(i -> { Rad r = i.getArgument(0); r.setId(8L); return r; });
        when(termini.save(any())).thenAnswer(i -> i.getArgument(0));
        when(grupe.save(any())).thenAnswer(i -> i.getArgument(0));
        when(zadaci.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void profesorVidiIOcenjeneRadoveIDetalj() throws Exception {
        rad.setStatus(StatusRada.OCENJEN); rad.setPoeni(85.0);
        when(radovi.findByPredmetIdAndStatus(4L, StatusRada.OCENJEN)).thenReturn(List.of(rad));
        // Postojeci upit dobija opcioni status; null znaci svi statusi.
        when(radovi.findByPredmetIdAndStatus(4L, null)).thenReturn(List.of(rad));
        mvc.perform(get("/api/rad/predmet/4").with(profesor()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("OCENJEN"));
        mvc.perform(get("/api/rad/predmet/4?status=OCENJEN").with(profesor()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].poeni").value(85));
        mvc.perform(get("/api/rad/8").with(profesor())).andExpect(status().isOk());
        mvc.perform(get("/api/rad/8").with(student())).andExpect(status().isOk());
    }

    @Test
    void tudjiKorisniciNeVideRadoveINeispravanStatusJe400() throws Exception {
        mvc.perform(get("/api/rad/8").with(tudjiProfesor())).andExpect(status().isForbidden());
        mvc.perform(get("/api/rad/8").with(jwt().jwt(j -> j.subject("drugi@raf.rs"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/rad/predmet/4").with(student())).andExpect(status().isForbidden());
        mvc.perform(get("/api/rad/predmet/4?status=NEPOSTOJI").with(profesor())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/rad/8")).andExpect(status().isUnauthorized());
    }

    @Test
    void profesorDobijaSamoSvojePredmete() throws Exception {
        mvc.perform(get("/api/predaje-predmet/moji").with(profesor()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].predmet.id").value(4));
        mvc.perform(get("/api/predaje-predmet/moji").with(tudjiProfesor()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/predaje-predmet/moji").with(student())).andExpect(status().isForbidden());
    }

    @Test
    void izmenaTerminaCuvaIdINoviRaspored() throws Exception {
        when(termini.postojiPreklapanje(2L, termin.getDatum(), LocalTime.of(11, 0), LocalTime.of(13, 0)))
                .thenReturn(true);
        mvc.perform(put("/api/termini/edit/6").with(profesor()).contentType("application/json")
                        .content(terminJson("11:00", "13:00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.vremePocetka").value("11:00:00"));
        assertEquals(LocalTime.of(11, 0), termin.getVremePocetak());
        verify(termini).postojiPreklapanjeOsim(6L, 2L, termin.getDatum(), LocalTime.of(11, 0), LocalTime.of(13, 0));
    }

    @Test
    void preklapanjeNeMenjaTermin() throws Exception {
        when(termini.postojiPreklapanjeOsim(6L, 2L, termin.getDatum(), LocalTime.of(11, 0), LocalTime.of(13, 0)))
                .thenReturn(true);
        mvc.perform(put("/api/termini/edit/6").with(profesor()).contentType("application/json")
                .content(terminJson("11:00", "13:00"))).andExpect(status().isConflict());
        assertEquals(LocalTime.of(10, 0), termin.getVremePocetak());
        verify(termini, never()).save(any());
    }

    @Test
    void noviPocetakNeSmeBitiUProslostiINeMenjaSePredmet() throws Exception {
        mvc.perform(put("/api/termini/edit/6").with(profesor()).contentType("application/json")
                .content(terminJson("08:00", "13:00"))).andExpect(status().isConflict());
        mvc.perform(put("/api/termini/edit/6").with(profesor()).contentType("application/json")
                .content(terminJson("11:00", "13:00").replace("\"predmetId\":4", "\"predmetId\":99")))
                .andExpect(status().isConflict());
        assertEquals(LocalTime.of(10, 0), termin.getVremePocetak());
        verify(termini, never()).save(any());
    }

    @Test
    void tudjiProfesorIProtekliTerminNeMoguDaMenjajuRaspored() throws Exception {
        mvc.perform(put("/api/termini/edit/6").with(tudjiProfesor()).contentType("application/json")
                .content(terminJson("11:00", "13:00"))).andExpect(status().isForbidden());
        vreme("10:00:00");
        mvc.perform(put("/api/termini/edit/6").with(profesor()).contentType("application/json")
                .content(terminJson("11:00", "13:00"))).andExpect(status().isConflict());
        assertEquals(LocalTime.of(10, 0), termin.getVremePocetak());
    }

    @Test
    void izmenaGrupeIZadatkaRadiPrePreuzimanja() throws Exception {
        mvc.perform(put("/api/raspored/grupe/3").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"G2\",\"terminId\":6}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.naziv").value("G2"));
        mvc.perform(put("/api/zadaci/5").with(profesor()).contentType("application/json")
                .content(zadatakJson())).andExpect(status().isOk())
                .andExpect(jsonPath("$.repoPutanja").value("https://github.com/test/novi"));
    }

    @Test
    void preuzimanjeDodeljujePutanjuOdEmailaPredmetaIDatuma() throws Exception {
        vreme("10:30:00");
        student.setEmail("mnikolic1223rn@raf.rs");
        termin.getPredmet().setNaziv("Razvoj softvera");
        when(studenti.pronadjiPoEmailu("mnikolic1223rn@raf.rs")).thenReturn(Optional.of(student));
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(post("/api/rad/5/preuzmi").with(jwt().jwt(j -> j.subject("mnikolic1223rn@raf.rs"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isCreated());
        var sacuvan = org.mockito.ArgumentCaptor.forClass(Rad.class);
        verify(radovi).save(sacuvan.capture());
        assertEquals("mnikolic1223rn_razvoj-softvera_150630", sacuvan.getValue().getRepoPutanjaResenja());
        assertEquals(StatusRada.PREUZET, sacuvan.getValue().getStatus());
        assertNull(sacuvan.getValue().getVremePredaje());
    }

    @ParameterizedTest
    @CsvSource({"2030-01-02,020130", "2030-12-31,311230"})
    void putanjaKoristiDatumTerminaUFormatuDDMMYY(String datum, String ocekivaniDatum) throws Exception {
        LocalDate dan = LocalDate.parse(datum); termin.setDatum(dan);
        when(clock.instant()).thenReturn(dan.atTime(10, 30).atZone(ZoneId.of("Europe/Belgrade")).toInstant());
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(post("/api/rad/5/preuzmi").with(student())).andExpect(status().isCreated());
        var sacuvan = org.mockito.ArgumentCaptor.forClass(Rad.class); verify(radovi).save(sacuvan.capture());
        assertEquals("student_rs_" + ocekivaniDatum, sacuvan.getValue().getRepoPutanjaResenja());
    }

    @Test
    void putanjaNormalizujeNazivPredmetaBezPromeneSamogPredmeta() throws Exception {
        vreme("10:30:00"); termin.getPredmet().setNaziv("  Računarske mreže / 2  ");
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(post("/api/rad/5/preuzmi").with(student())).andExpect(status().isCreated());
        var sacuvan = org.mockito.ArgumentCaptor.forClass(Rad.class); verify(radovi).save(sacuvan.capture());
        assertEquals("student_racunarske-mreze-2_150630", sacuvan.getValue().getRepoPutanjaResenja());
        assertEquals("  Računarske mreže / 2  ", termin.getPredmet().getNaziv());
    }

    @Test
    void neispravniPodaciZaPutanjuNeUpisujuRad() throws Exception {
        vreme("10:30:00"); termin.setPredmet(null);
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(post("/api/rad/5/preuzmi").with(student())).andExpect(status().isConflict());
        verify(radovi, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"repoPutanjaResenja\":null}", "{\"repoPutanjaResenja\":\"\"}",
            "{\"repoPutanjaResenja\":\"   \"}"})
    void predajaBezRucnePutanjeAutomatskiFormiraPutanju(String telo) throws Exception {
        vreme("10:30:00");
        mvc.perform(post("/api/rad/8/predaj").with(student()).contentType("application/json").content(telo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repoPutanjaResenja")
                        .value("student_rs_150630"))
                .andExpect(jsonPath("$.status").value("PREDAT"));
        assertEquals(LocalDateTime.of(2030, 6, 15, 10, 30), rad.getVremePredaje());
    }

    @Test
    void predajaBezPutanjeCuvaVecDodeljenoResenje() throws Exception {
        vreme("10:30:00"); rad.setRepoPutanjaResenja("https://github.com/student/moje-resenje");
        mvc.perform(post("/api/rad/8/predaj").with(student()).contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repoPutanjaResenja")
                        .value("https://github.com/student/moje-resenje"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"student_rs_150630", "https://github.com/student/postojeci-repo"})
    void poslataPutanjaNeMozeDaPromeniDodeljenuPutanju(String postojeca) throws Exception {
        vreme("10:30:00"); rad.setRepoPutanjaResenja(postojeca);
        mvc.perform(post("/api/rad/8/predaj").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"tudja-putanja\",\"poeni\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repoPutanjaResenja").value(postojeca))
                .andExpect(jsonPath("$.status").value("PREDAT"));
        assertEquals(postojeca, rad.getRepoPutanjaResenja());
        assertNull(rad.getPoeni());
    }

    @Test
    void predajaBezTelaIZaglavljaZadrzavaPutanjuIPoslePreimenovanjaPredmeta() throws Exception {
        vreme("10:30:00"); rad.setRepoPutanjaResenja("student_rs_150630");
        termin.getPredmet().setNaziv("Promenjen naziv");
        mvc.perform(post("/api/rad/8/predaj").with(student()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repoPutanjaResenja").value("student_rs_150630"));
    }

    @Test
    void preuzetZadatakINjegovaGrupaSeNeMenjaju() throws Exception {
        when(radovi.existsByZadatak_Id(5L)).thenReturn(true);
        when(radovi.existsByZadatak_Grupa_Id(3L)).thenReturn(true);
        mvc.perform(put("/api/zadaci/5").with(profesor()).contentType("application/json")
                .content(zadatakJson())).andExpect(status().isConflict());
        mvc.perform(put("/api/raspored/grupe/3").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"G2\",\"terminId\":6}")).andExpect(status().isConflict());
        assertEquals("Z1", zadatak.getNaziv());
        assertEquals("G1", grupa.getNaziv());
        verify(zadaci, never()).save(any());
        verify(grupe, never()).save(any());
    }

    @Test
    void grupaSaPrijavamaNeMenjaTermin() throws Exception {
        Termin drugi = new Termin(); drugi.setId(12L); drugi.setPredmet(termin.getPredmet());
        drugi.setDatum(termin.getDatum()); drugi.setVremePocetak(LocalTime.of(13, 0)); drugi.setVremeKraj(LocalTime.of(15, 0));
        when(termini.findById(12L)).thenReturn(Optional.of(drugi));
        when(prijave.existsByGrupa_Id(3L)).thenReturn(true);
        mvc.perform(put("/api/raspored/grupe/3").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"G2\",\"terminId\":12}")).andExpect(status().isConflict());
        assertEquals(6L, grupa.getTermin().getId());
    }

    @Test
    void praznaGrupaMozeDaPromeniTermin() throws Exception {
        Termin drugi = new Termin(); drugi.setId(12L); drugi.setPredmet(termin.getPredmet());
        drugi.setDatum(termin.getDatum()); drugi.setVremePocetak(LocalTime.of(13, 0)); drugi.setVremeKraj(LocalTime.of(15, 0));
        when(termini.findById(12L)).thenReturn(Optional.of(drugi));
        mvc.perform(put("/api/raspored/grupe/3").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"G2\",\"terminId\":12}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.termin.id").value(12));
    }

    @Test
    void zadatakNeMenjaGrupuINeUzimaZauzetuPutanju() throws Exception {
        mvc.perform(put("/api/zadaci/5").with(profesor()).contentType("application/json")
                .content(zadatakJson().replace("\"grupaId\":3", "\"grupaId\":99"))).andExpect(status().isConflict());
        when(zadaci.existsByRepoPutanja("https://github.com/test/novi")).thenReturn(true);
        mvc.perform(put("/api/zadaci/5").with(profesor()).contentType("application/json")
                .content(zadatakJson())).andExpect(status().isConflict());
        assertEquals("Z1", zadatak.getNaziv());
        verify(zadaci, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"/api/zadaci/5", "/api/raspored/grupe/3", "/api/termini/edit/6"})
    void studentITudjiProfesorNeMoguDaMenjajuPodatke(String putanja) throws Exception {
        mvc.perform(put(putanja).with(student()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(putanja).with(tudjiProfesor()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        // @Valid se izvrsava pre autorizacije metode; potpun tudji zahtev se proverava ispod.
        String telo = putanja.contains("zadaci") ? zadatakJson() : putanja.contains("grupe")
                ? "{\"naziv\":\"G2\",\"terminId\":6}" : terminJson("11:00", "13:00");
        mvc.perform(put(putanja).with(tudjiProfesor()).contentType("application/json").content(telo))
                .andExpect(status().isForbidden());
        verify(zadaci, never()).save(any()); verify(grupe, never()).save(any()); verify(termini, never()).save(any());
    }

    @Test
    void odjavaBriseSamoSopstvenuPrijavu() throws Exception {
        PrijavljenStudent prijava = new PrijavljenStudent(); prijava.setId(10L);
        prijava.setStudent(student); prijava.setGrupa(grupa);
        when(prijave.getStudentByEmail("student@raf.rs")).thenReturn(List.of(prijava));
        mvc.perform(delete("/api/prijave/grupa/3").with(student())).andExpect(status().isNoContent());
        verify(prijave).delete(prijava);
    }

    @Test
    void nepostojecaPrijavaSeNeBrise() throws Exception {
        mvc.perform(delete("/api/prijave/grupa/3").with(student())).andExpect(status().isNotFound());
        verify(prijave, never()).delete(any());
    }

    @Test
    void prijavaIIzmenaGrupePonovoCitajuZakljucanZapis() throws Exception {
        when(grupe.findByIdZaIzmenu(3L)).thenReturn(Optional.empty());
        when(prijave.save(any())).thenAnswer(i -> { PrijavljenStudent p = i.getArgument(0); p.setId(10L); return p; });
        mvc.perform(post("/api/prijave/grupa/3").with(student())).andExpect(status().isNotFound());
        mvc.perform(put("/api/raspored/grupe/3").with(profesor()).contentType("application/json")
                .content("{\"naziv\":\"G2\",\"terminId\":6}")).andExpect(status().isNotFound());
        verify(prijave, never()).save(any()); verify(grupe, never()).save(any());
    }

    @Test
    void preuzimanjeIIzmenaZadatkaPonovoCitajuZakljucanZapis() throws Exception {
        when(zadaci.findByIdZaIzmenu(5L)).thenReturn(Optional.empty());
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        vreme("10:30:00");
        mvc.perform(post("/api/rad/5/preuzmi").with(student())).andExpect(status().isNotFound());
        mvc.perform(put("/api/zadaci/5").with(profesor()).contentType("application/json")
                .content(zadatakJson())).andExpect(status().isNotFound());
        verify(radovi, never()).save(any()); verify(zadaci, never()).save(any());
    }

    @Test
    void odjavaSeOdbijaNaPocetkuINakonPreuzimanja() throws Exception {
        PrijavljenStudent prijava = new PrijavljenStudent(); prijava.setStudent(student); prijava.setGrupa(grupa);
        when(prijave.getStudentByEmail("student@raf.rs")).thenReturn(List.of(prijava));
        vreme("10:00:00");
        mvc.perform(delete("/api/prijave/grupa/3").with(student())).andExpect(status().isConflict());
        vreme("09:00:00");
        when(radovi.existsByStudent_IdAndZadatak_Grupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(delete("/api/prijave/grupa/3").with(student())).andExpect(status().isConflict());
        mvc.perform(delete("/api/prijave/grupa/3").with(profesor())).andExpect(status().isForbidden());
        verify(prijave, never()).delete(any());
    }

    @Test
    void nepotpunTerminOdbijaPrijavuIRadBezUpisa() throws Exception {
        termin.setVremeKraj(null);
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(post("/api/rad/5/preuzmi").with(student())).andExpect(status().isConflict());
        mvc.perform(post("/api/rad/8/predaj").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"https://github.com/test/resenje\"}"))
                .andExpect(status().isConflict());
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(false);
        mvc.perform(post("/api/prijave/grupa/3").with(student())).andExpect(status().isConflict());
        verify(radovi, never()).save(any()); verify(prijave, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"09:59:59,201", "10:00:00,409", "12:00:00,409"})
    void prijavaSeZatvaraNaPocetku(String vreme, int status) throws Exception {
        vreme(vreme);
        when(prijave.save(any())).thenAnswer(i -> { PrijavljenStudent p = i.getArgument(0); p.setId(10L); return p; });
        mvc.perform(post("/api/prijave/grupa/3").with(student())).andExpect(status().is(status));
    }

    @ParameterizedTest
    @CsvSource({"09:59:59,409", "10:00:00,201", "10:30:00,201", "10:30:01,409", "11:59:59,409", "12:00:00,409"})
    void preuzimanjeSamoUPrvihTridesetMinutaTermina(String vreme, int status) throws Exception {
        vreme(vreme);
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        mvc.perform(post("/api/rad/5/preuzmi").with(student())).andExpect(status().is(status));
    }

    @ParameterizedTest
    @CsvSource({"09:59:59,409", "10:00:00,200", "10:30:00,200", "10:30:01,409"})
    void studentVidiZadatakSamoDokMozeDaGaPreuzme(String vreme, int status) throws Exception {
        vreme(vreme);
        when(prijave.existsByStudent_IdAndGrupa_Id(7L, 3L)).thenReturn(true);
        when(zadaci.pronadjiPoGrupi(3L)).thenReturn(Optional.of(zadatak));
        mvc.perform(get("/api/zadaci/grupa/3").with(student())).andExpect(status().is(status));
    }

    @ParameterizedTest
    @CsvSource({"09:59:59,409", "10:00:00,200", "11:59:59,200", "12:00:00,200", "12:05:00,200", "12:05:01,409"})
    void predajaDoPetMinutaPosleKrajaTermina(String vreme, int status) throws Exception {
        vreme(vreme);
        mvc.perform(post("/api/rad/8/predaj").with(student()).contentType("application/json")
                .content("{\"repoPutanjaResenja\":\"https://github.com/test/resenje\"}"))
                .andExpect(status().is(status));
        assertEquals(status == 200 ? StatusRada.PREDAT : StatusRada.PREUZET, rad.getStatus());
    }

    @Test
    void promenaLozinkeMenjaLoginIHashProfila() throws Exception {
        Korisnik nalog = nalog();
        mvc.perform(put("/api/auth/lozinka").with(student()).contentType("application/json")
                .content("{\"staraLozinka\":\"stara123\",\"novaLozinka\":\"novaLozinka123\"}"))
                .andExpect(status().isNoContent());
        assertTrue(encoder.matches("novaLozinka123", nalog.getPasswordHash()));
        assertEquals(nalog.getPasswordHash(), student.getPasswordHash());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"student@raf.rs\",\"lozinka\":\"stara123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"student@raf.rs\",\"lozinka\":\"novaLozinka123\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"pogresna,novaLozinka123,401", "stara123,kratka,400"})
    void neispravnaPromenaNeMenjaHash(String stara, String nova, int status) throws Exception {
        Korisnik nalog = nalog(); String hash = nalog.getPasswordHash();
        mvc.perform(put("/api/auth/lozinka").with(student()).contentType("application/json")
                .content("{\"staraLozinka\":\"%s\",\"novaLozinka\":\"%s\"}".formatted(stara, nova)))
                .andExpect(status().is(status));
        assertEquals(hash, nalog.getPasswordHash());
        assertEquals(hash, student.getPasswordHash());
    }

    @Test
    void bcryptGranicaSeProveravaPoUtf8Bajtovima() throws Exception {
        Korisnik nalog = nalog(); String hash = nalog.getPasswordHash();
        mvc.perform(put("/api/auth/lozinka").with(student()).contentType("application/json")
                .content("{\"staraLozinka\":\"stara123\",\"novaLozinka\":\"%s\"}".formatted("ž".repeat(37))))
                .andExpect(status().isBadRequest());
        assertEquals(hash, nalog.getPasswordHash());
    }

    @Test
    void predugaStaraLozinkaSeOdbijaBezGreskeServera() throws Exception {
        Korisnik nalog = nalog(); String hash = nalog.getPasswordHash();
        mvc.perform(put("/api/auth/lozinka").with(student()).contentType("application/json")
                .content("{\"staraLozinka\":\"%s\",\"novaLozinka\":\"novaLozinka123\"}".formatted("ž".repeat(37))))
                .andExpect(status().isBadRequest());
        assertEquals(hash, nalog.getPasswordHash());
        verify(korisnici, never()).save(any());
    }

    @Test
    void profesorMenjaLozinkuSvogProfila() throws Exception {
        Profesor profesor = new Profesor(); profesor.setId(9L);
        Korisnik nalog = new Korisnik(); nalog.setUloga(Uloga.PROFESOR); nalog.setProfesor(profesor);
        nalog.setPasswordHash(encoder.encode("stara123"));
        when(korisnici.findByEmail("profesor@raf.rs")).thenReturn(Optional.of(nalog));
        mvc.perform(put("/api/auth/lozinka").with(profesor()).contentType("application/json")
                .content("{\"staraLozinka\":\"stara123\",\"novaLozinka\":\"novaLozinka123\"}"))
                .andExpect(status().isNoContent());
        assertTrue(encoder.matches("novaLozinka123", profesor.getPasswordHash()));
        assertEquals(nalog.getPasswordHash(), profesor.getPasswordHash());
    }

    @Test
    void promenaLozinkeZahtevaPrijavuIPovezanProfil() throws Exception {
        String telo = "{\"staraLozinka\":\"stara123\",\"novaLozinka\":\"novaLozinka123\"}";
        mvc.perform(put("/api/auth/lozinka").contentType("application/json").content(telo))
                .andExpect(status().isUnauthorized());
        Korisnik nalog = nalog(); nalog.setStudent(null); String hash = nalog.getPasswordHash();
        mvc.perform(put("/api/auth/lozinka").with(student()).contentType("application/json").content(telo))
                .andExpect(status().isConflict());
        assertEquals(hash, nalog.getPasswordHash());
        verify(korisnici, never()).save(any());
    }

    private Korisnik nalog() {
        Korisnik nalog = new Korisnik(); nalog.setId(11L); nalog.setEmail("student@raf.rs");
        nalog.setUloga(Uloga.STUDENT); nalog.setStudent(student);
        nalog.setPasswordHash(encoder.encode("stara123")); student.setPasswordHash(nalog.getPasswordHash());
        when(korisnici.findByEmail("student@raf.rs")).thenReturn(Optional.of(nalog));
        return nalog;
    }

    private void vreme(String time) {
        when(clock.instant()).thenReturn(LocalDateTime.parse("2030-06-15T" + time)
                .atZone(ZoneId.of("Europe/Belgrade")).toInstant());
    }
    private String terminJson(String pocetak, String kraj) {
        return "{\"datum\":\"2030-06-15\",\"vremePocetak\":\"%s\",\"vremeKraja\":\"%s\",\"ucionicaId\":2,\"predmetId\":4,\"rokId\":1}"
                .formatted(pocetak, kraj);
    }
    private String zadatakJson() {
        return "{\"naziv\":\"Novi\",\"grupaId\":3,\"repoPutanja\":\"https://github.com/test/novi\"}";
    }
    private JwtRequestPostProcessor student() {
        return jwt().jwt(j -> j.subject("student@raf.rs")).authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }
    private JwtRequestPostProcessor profesor() {
        return jwt().jwt(j -> j.subject("profesor@raf.rs")).authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    }
    private JwtRequestPostProcessor tudjiProfesor() {
        return jwt().jwt(j -> j.subject("drugi@raf.rs")).authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    }
}
