package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.config.SecurityConfig;
import com.inzinjeri.rs_projekat.exceptions.GlobalExceptionHandler;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.NalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
        pattern = "com\\.inzinjeri\\.rs_projekat\\.controllers\\.(Nalog|Student|Profesor)Controller"))
@Import({SecurityConfig.class, NalogService.class, GlobalExceptionHandler.class})
class AccountCrudHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private StudentRepository studenti;
    @MockitoBean private ProfesorRepository profesori;
    @MockitoBean private KorisnikRepository korisnici;
    @MockitoBean private RadRepository radovi;
    @MockitoBean private PrijavljenStudentRepository prijave;
    @MockitoBean private PredajePredmetRepository predaje;
    private Student student;
    private Profesor profesor;
    private Korisnik nalog;

    @BeforeEach
    void podaci() {
        student = new Student(); student.setId(1L); student.setIme("Ana"); student.setPrezime("Ilic");
        student.setEmail("student@raf.rs"); student.setSmer("RN"); student.setBrIndeksa("1/2023");
        student.setGodUpisa(2023); student.setPasswordHash("tajni-hash");
        profesor = new Profesor(); profesor.setId(2L); profesor.setIme("Milan"); profesor.setPrezime("Ilic");
        profesor.setEmail("profesor@raf.rs"); profesor.setPasswordHash("tajni-hash");
        nalog = new Korisnik(); nalog.setId(10L); nalog.setEmail(student.getEmail());
        nalog.setUloga(Uloga.STUDENT); nalog.setStudent(student); nalog.setPasswordHash("tajni-hash");
        when(studenti.findById(1L)).thenReturn(Optional.of(student));
        when(studenti.pronadjiPoEmailu(student.getEmail())).thenReturn(Optional.of(student));
        when(profesori.findById(2L)).thenReturn(Optional.of(profesor));
        when(profesori.findByEmailIgnoreCase(profesor.getEmail())).thenReturn(Optional.of(profesor));
        when(korisnici.findById(10L)).thenReturn(Optional.of(nalog));
        when(korisnici.findByEmail(student.getEmail())).thenReturn(Optional.of(nalog));
        when(studenti.findAll()).thenReturn(List.of(student));
        when(profesori.findAll()).thenReturn(List.of(profesor));
        when(korisnici.findAll()).thenReturn(List.of(nalog));
        when(studenti.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(profesori.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(korisnici.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void profesorCitaNalogeBezLozinki() throws Exception {
        mvc.perform(get("/api/nalozi").with(profesorAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10)).andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/nalozi/10").with(profesorAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@raf.rs"));
        mvc.perform(get("/api/nalozi/me").with(studentAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10)).andExpect(jsonPath("$.uloga").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void studentCitaSamoSvojProfilAProfesoriSuVidljivi() throws Exception {
        mvc.perform(get("/api/studenti/me").with(studentAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/studenti/1").with(studentAuth())).andExpect(status().isOk());
        mvc.perform(get("/api/studenti/2").with(studentAuth())).andExpect(status().isForbidden());
        mvc.perform(get("/api/profesori").with(studentAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/profesori/2").with(studentAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/profesori/me").with(profesorAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
        mvc.perform(get("/api/studenti").with(profesorAuth())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/nalozi", "/api/nalozi/10", "/api/studenti", "/api/profesori/me"})
    void studentNeCitaAdministrativnePodatke(String path) throws Exception {
        mvc.perform(get(path).with(studentAuth())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/studenti", "/api/profesori", "/api/nalozi"})
    void studentNeMenjaProfileINaloge(String path) throws Exception {
        mvc.perform(post(path).with(studentAuth()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(path + "/edit/1").with(studentAuth()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path + "/delete/1").with(studentAuth())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/nalozi/999", "/api/studenti/999", "/api/profesori/999"})
    void nepostojeciPodaciVracaju404(String path) throws Exception {
        mvc.perform(get(path).with(profesorAuth())).andExpect(status().isNotFound());
    }

    @Test
    void izmenaStudentskogProfilaCuvaEmailILozinku() throws Exception {
        mvc.perform(put("/api/studenti/edit/1").with(profesorAuth()).contentType("application/json").content("""
                {"ime":" Jovana ","prezime":" Jovic ","smer":"ri","brIndeksa":"002/2024","godUpisa":2024}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.ime").value("Jovana"))
                .andExpect(jsonPath("$.brIndeksa").value("2/2024"))
                .andExpect(jsonPath("$.email").value("student@raf.rs"));
        assertEquals("tajni-hash", student.getPasswordHash());
    }

    @Test
    void izmenaProfesoraCuvaEmailILozinku() throws Exception {
        mvc.perform(put("/api/profesori/edit/2").with(profesorAuth()).contentType("application/json")
                .content("{\"ime\":\" Petar \",\"prezime\":\" Petrovic \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ime").value("Petar"))
                .andExpect(jsonPath("$.email").value("profesor@raf.rs"));
        assertEquals("tajni-hash", profesor.getPasswordHash());
    }

    @Test
    void novePutanjeKreirajuProfilINalog() throws Exception {
        mvc.perform(post("/api/studenti").with(profesorAuth()).contentType("application/json").content("""
                {"ime":"Ana","prezime":"Ilic","smer":"RN","brIndeksa":"2/2023","godUpisa":2023}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("ailic223rn@raf.rs"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/profesori").with(profesorAuth()).contentType("application/json")
                .content("{\"ime\":\"Petar\",\"prezime\":\"Petrovic\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("ppetrovic@raf.rs"));
        verify(korisnici, times(2)).saveAndFlush(any());
    }

    @Test
    void neuskladjenaGodinaIndeksaSeOdbija() throws Exception {
        mvc.perform(put("/api/studenti/edit/1").with(profesorAuth()).contentType("application/json").content("""
                {"ime":"Ana","prezime":"Ilic","smer":"RN","brIndeksa":"2/2024","godUpisa":2023}
                """)).andExpect(status().isBadRequest());
        verify(studenti, never()).saveAndFlush(any());
    }

    @Test
    void zauzetIndeksSeOdbija() throws Exception {
        Student drugi = new Student(); drugi.setId(3L);
        when(studenti.pronadjiPoBrojuIndeksa("2/2023")).thenReturn(Optional.of(drugi));
        mvc.perform(put("/api/studenti/edit/1").with(profesorAuth()).contentType("application/json").content("""
                {"ime":"Ana","prezime":"Ilic","smer":"RN","brIndeksa":"2/2023","godUpisa":2023}
                """)).andExpect(status().isConflict());
    }

    @Test
    void izmenaEmailaSinhronizujeProfilINeIzlazeHash() throws Exception {
        mvc.perform(put("/api/nalozi/edit/10").with(profesorAuth()).contentType("application/json")
                .content("{\"email\":\"novo@raf.rs\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("novo@raf.rs"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertEquals("novo@raf.rs", student.getEmail());
        assertEquals("tajni-hash", nalog.getPasswordHash());
    }

    @Test
    void zauzetEmailSeOdbijaBezIzmene() throws Exception {
        when(profesori.existsByEmailIgnoreCase("zauzet@raf.rs")).thenReturn(true);
        mvc.perform(put("/api/nalozi/edit/10").with(profesorAuth()).contentType("application/json")
                .content("{\"email\":\"zauzet@raf.rs\"}"))
                .andExpect(status().isConflict());
        assertEquals("student@raf.rs", student.getEmail());
    }

    @Test
    void neispravanEmailSeOdbija() throws Exception {
        mvc.perform(put("/api/nalozi/edit/10").with(profesorAuth()).contentType("application/json")
                .content("{\"email\":\"neispravan\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brisanjeNalogaOstavljaProfil() throws Exception {
        mvc.perform(delete("/api/nalozi/delete/10").with(profesorAuth())).andExpect(status().isNoContent());
        verify(korisnici).delete(nalog);
        verify(studenti, never()).delete(any());
        verify(profesori, never()).delete(any());
    }

    @Test
    void profesorBriseNepovezaneProfile() throws Exception {
        mvc.perform(delete("/api/studenti/delete/1").with(profesorAuth())).andExpect(status().isNoContent());
        verify(studenti).delete(student);
        mvc.perform(delete("/api/profesori/delete/2").with(profesorAuth())).andExpect(status().isNoContent());
        verify(profesori).delete(profesor);
    }

    @Test
    void brisanjeStudentaPrvoBrisePovezaniNalog() throws Exception {
        when(korisnici.findByStudent_Id(1L)).thenReturn(Optional.of(nalog));
        mvc.perform(delete("/api/studenti/delete/1").with(profesorAuth())).andExpect(status().isNoContent());
        var redosled = inOrder(korisnici, studenti);
        redosled.verify(korisnici).delete(nalog);
        redosled.verify(korisnici).flush();
        redosled.verify(studenti).delete(student);
    }

    @Test
    void brisanjeProfesoraPrvoBrisePovezaniNalog() throws Exception {
        Korisnik profesorskiNalog = new Korisnik(); profesorskiNalog.setId(11L);
        profesorskiNalog.setProfesor(profesor);
        when(korisnici.findByProfesor_Id(2L)).thenReturn(Optional.of(profesorskiNalog));
        mvc.perform(delete("/api/profesori/delete/2").with(profesorAuth())).andExpect(status().isNoContent());
        var redosled = inOrder(korisnici, profesori);
        redosled.verify(korisnici).delete(profesorskiNalog);
        redosled.verify(korisnici).flush();
        redosled.verify(profesori).delete(profesor);
    }

    @Test
    void studentSaRadovimaNeMozeDaSeObrise() throws Exception {
        when(radovi.existsByStudent_Id(1L)).thenReturn(true);
        mvc.perform(delete("/api/studenti/delete/1").with(profesorAuth())).andExpect(status().isConflict());
        verify(studenti, never()).delete(any());
        verify(korisnici, never()).delete(any());
    }

    @Test
    void studentSaPrijavamaNeMozeDaSeObrise() throws Exception {
        when(prijave.existsByStudent_Id(1L)).thenReturn(true);
        mvc.perform(delete("/api/studenti/delete/1").with(profesorAuth())).andExpect(status().isConflict());
        verify(studenti, never()).delete(any());
        verify(korisnici, never()).delete(any());
    }

    @Test
    void profesorSaPredmetimaNeMozeDaSeObrise() throws Exception {
        when(predaje.existsByProfesor_Id(2L)).thenReturn(true);
        mvc.perform(delete("/api/profesori/delete/2").with(profesorAuth())).andExpect(status().isConflict());
        verify(profesori, never()).delete(any());
        verify(korisnici, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/nalozi/delete/999", "/api/studenti/delete/999", "/api/profesori/delete/999"})
    void brisanjeNepostojecegPodatkaVraca404(String path) throws Exception {
        mvc.perform(delete(path).with(profesorAuth())).andExpect(status().isNotFound());
    }

    @Test
    void izmenaEmailaProfesoraSinhronizujeProfil() throws Exception {
        nalog.setStudent(null); nalog.setProfesor(profesor); nalog.setUloga(Uloga.PROFESOR);
        nalog.setEmail(profesor.getEmail());
        mvc.perform(put("/api/nalozi/edit/10").with(profesorAuth()).contentType("application/json")
                .content("{\"email\":\"novi.profesor@raf.rs\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profesorId").value(2));
        assertEquals("novi.profesor@raf.rs", profesor.getEmail());
    }

    @Test
    void sopstveniIndeksIEmailMoguDaOstanuIsti() throws Exception {
        when(studenti.pronadjiPoBrojuIndeksa("1/2023")).thenReturn(Optional.of(student));
        when(korisnici.existsByEmailIgnoreCase("student@raf.rs")).thenReturn(true);
        when(studenti.existsByEmailIgnoreCase("student@raf.rs")).thenReturn(true);
        mvc.perform(put("/api/studenti/edit/1").with(profesorAuth()).contentType("application/json").content("""
                {"ime":"Ana","prezime":"Ilic","smer":"RN","brIndeksa":"1/2023","godUpisa":2023}
                """)).andExpect(status().isOk());
        mvc.perform(put("/api/nalozi/edit/10").with(profesorAuth()).contentType("application/json")
                .content("{\"email\":\"student@raf.rs\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void stvarniTokenPrestajeDaVaziPosleBrisanjaNaloga() throws Exception {
        String token = token(10L, "student@raf.rs", "STUDENT");
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        when(korisnici.findById(10L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void stariTokenNeOzivljavaKadaSeEmailPonovoKreira() throws Exception {
        Korisnik novi = new Korisnik(); novi.setId(20L); novi.setEmail("student@raf.rs"); novi.setUloga(Uloga.STUDENT);
        when(korisnici.findById(10L)).thenReturn(Optional.empty());
        when(korisnici.findByEmail("student@raf.rs")).thenReturn(Optional.of(novi));
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token(10L, "student@raf.rs", "STUDENT")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stvarniTokenZahtevaAktuelniEmailIUlogu() throws Exception {
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token(10L, "stari@raf.rs", "STUDENT")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token(10L, "student@raf.rs", "PROFESOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void izmenaEmailaOdmahPonistavaStariToken() throws Exception {
        String token = token(10L, "student@raf.rs", "STUDENT");
        mvc.perform(put("/api/nalozi/edit/10").with(profesorAuth()).contentType("application/json")
                .content("{\"email\":\"novo@raf.rs\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenBezIdentitetaIliSaTekstualnimIdentitetomSeOdbija() throws Exception {
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token(null, "student@raf.rs", "STUDENT")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profesori").header("Authorization", "Bearer " + token("10", "student@raf.rs", "STUDENT")))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor profesorAuth() {
        return jwt().jwt(j -> j.subject("profesor@raf.rs")).authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    }

    private RequestPostProcessor studentAuth() {
        return jwt().jwt(j -> j.subject("student@raf.rs")).authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    private String token(Object id, String email, String role) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(email).issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("uloga", role);
        if (id != null) claims.claim("korisnikId", id);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }
}
