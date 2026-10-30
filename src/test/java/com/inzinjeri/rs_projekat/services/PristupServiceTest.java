package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PristupServiceTest {
    private final PredajePredmetRepository predmeti = mock(PredajePredmetRepository.class);
    private final TerminRepository termini = mock(TerminRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final RadRepository radovi = mock(RadRepository.class);
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final ZadatakRepository zadaci = mock(ZadatakRepository.class);
    private final PristupService pristup = new PristupService(predmeti, termini, grupe, radovi, studenti, prijave, zadaci);
    private final Authentication student = auth("student@test.rs", "STUDENT");
    private final Authentication profesor = auth("profesor@test.rs", "PROFESOR");
    private final Predmet predmet = new Predmet();
    private final Termin termin = new Termin();
    private final Grupa grupa = new Grupa();
    private final Zadatak zadatak = new Zadatak();
    private final Rad rad = new Rad();

    @BeforeEach
    void podaci() {
        predmet.setId(4L);
        termin.setPredmet(predmet);
        grupa.setId(3L);
        grupa.setTermin(termin);
        zadatak.setGrupa(grupa);
        Student profil = new Student();
        profil.setId(1L);
        profil.setEmail("student@test.rs");
        rad.setStudent(profil);
        rad.setZadatak(zadatak);
        PredajePredmet veza = new PredajePredmet();
        veza.setPredmet(predmet);
        when(predmeti.getPredmetiZaPredavaca("profesor@test.rs")).thenReturn(List.of(veza));
        when(termini.findById(7L)).thenReturn(Optional.of(termin));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(zadaci.findById(5L)).thenReturn(Optional.of(zadatak));
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(studenti.pronadjiPoEmailu("student@test.rs")).thenReturn(Optional.of(profil));
        when(prijave.existsByStudent_IdAndGrupa_Id(1L, 3L)).thenReturn(true);
    }

    @Test
    void profesorPristupaSamoPredmetimaKojePredajeINjihovimResursima() {
        assertTrue(pristup.profesorPredajePredmet(profesor, 4L));
        assertTrue(pristup.profesorPredajePredmetTermina(profesor, 7L));
        assertTrue(pristup.profesorPredajePredmetGrupe(profesor, 3L));
        assertTrue(pristup.profesorPredajePredmetRada(profesor, 9L));
        assertFalse(pristup.profesorPredajePredmet(profesor, 44L));
        Authentication drugi = auth("drugi.profesor@test.rs", "PROFESOR");
        assertFalse(pristup.profesorPredajePredmetTermina(drugi, 7L));
        assertFalse(pristup.profesorPredajePredmetGrupe(drugi, 3L));
        assertFalse(pristup.profesorPredajePredmetRada(drugi, 9L));
    }

    @Test
    void studentPristupaSvomRaduIGrupi() {
        assertTrue(pristup.studentJeVlasnikRada(student, 9L));
        assertTrue(pristup.studentPripadaGrupi(student, 3L));
        assertTrue(pristup.studentImaPristupZadatku(student, 5L));
        Authentication drugi = auth("drugi.student@test.rs", "STUDENT");
        assertFalse(pristup.studentJeVlasnikRada(drugi, 9L));
        assertFalse(pristup.studentPripadaGrupi(drugi, 3L));
        assertFalse(pristup.studentImaPristupZadatku(drugi, 5L));
        when(prijave.existsByStudent_IdAndGrupa_Id(1L, 3L)).thenReturn(false);
        assertFalse(pristup.studentPripadaGrupi(student, 3L));
        assertFalse(pristup.studentImaPristupZadatku(student, 5L));
    }

    @Test
    void pogresnaUlogaNeDobijaPristupNiSaEmailomPostojecegProfila() {
        Authentication lazniStudent = auth("student@test.rs", "PROFESOR");
        assertFalse(pristup.studentJeVlasnikRada(lazniStudent, 9L));
        assertFalse(pristup.studentPripadaGrupi(lazniStudent, 3L));
        assertFalse(pristup.studentImaPristupZadatku(lazniStudent, 5L));
        Authentication lazniProfesor = auth("profesor@test.rs", "STUDENT");
        assertFalse(pristup.profesorPredajePredmet(lazniProfesor, 4L));
        assertFalse(pristup.profesorPredajePredmetTermina(lazniProfesor, 7L));
        assertFalse(pristup.profesorPredajePredmetGrupe(lazniProfesor, 3L));
        assertFalse(pristup.profesorPredajePredmetRada(lazniProfesor, 9L));
    }

    @Test
    void bezPotvrdjenogIdentitetaIliIdaNemaPristupa() {
        List<BiPredicate<Authentication, Long>> provere = List.of(
                pristup::profesorPredajePredmet, pristup::profesorPredajePredmetTermina,
                pristup::profesorPredajePredmetGrupe, pristup::profesorPredajePredmetRada,
                pristup::studentJeVlasnikRada, pristup::studentPripadaGrupi, pristup::studentImaPristupZadatku);
        Authentication nepotvrdjen = new UsernamePasswordAuthenticationToken("student@test.rs", null);
        Authentication anoniman = new AnonymousAuthenticationToken("test", "student@test.rs", student.getAuthorities());
        for (var provera : provere) {
            assertFalse(provera.test(null, 3L));
            assertFalse(provera.test(nepotvrdjen, 3L));
            assertFalse(provera.test(anoniman, 3L));
            assertFalse(provera.test(student, null));
            assertFalse(provera.test(profesor, null));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 999})
    void nepostojeciResursiNeOdobravajuPristup(long id) {
        assertFalse(pristup.profesorPredajePredmetTermina(profesor, id));
        assertFalse(pristup.profesorPredajePredmetGrupe(profesor, id));
        assertFalse(pristup.profesorPredajePredmetRada(profesor, id));
        assertFalse(pristup.studentJeVlasnikRada(student, id));
        assertFalse(pristup.studentPripadaGrupi(student, id));
        assertFalse(pristup.studentImaPristupZadatku(student, id));
    }

    @Test
    void nepotpuneVezeNeOdobravajuPristup() {
        rad.setStudent(null);
        rad.setZadatak(null);
        assertFalse(pristup.studentJeVlasnikRada(student, 9L));
        assertFalse(pristup.profesorPredajePredmetRada(profesor, 9L));
        zadatak.setGrupa(null);
        assertFalse(pristup.studentImaPristupZadatku(student, 5L));
        grupa.setTermin(null);
        assertFalse(pristup.profesorPredajePredmetGrupe(profesor, 3L));
        termin.setPredmet(null);
        assertFalse(pristup.profesorPredajePredmetTermina(profesor, 7L));
    }

    private static Authentication auth(String email, String uloga) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + uloga)));
    }
}
