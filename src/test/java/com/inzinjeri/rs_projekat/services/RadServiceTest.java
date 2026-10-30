package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.OcenaRadaRequest;
import com.inzinjeri.rs_projekat.controllers.request.PredajaRadaRequest;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.PredajePredmet;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.model.Termin;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RadServiceTest {

    private final RadRepository radovi = mock(RadRepository.class);
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final ZadatakRepository zadaci = mock(ZadatakRepository.class);
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final PredajePredmetRepository predmeti = mock(PredajePredmetRepository.class);
    private final RadService service = new RadService(radovi, studenti, zadaci, prijave, predmeti);
    private final Authentication auth = new UsernamePasswordAuthenticationToken("student@example.com", null);
    private final Authentication profesor = new UsernamePasswordAuthenticationToken("profesor@example.com", null);

    @Test
    void profesorVidiPredateRadovePredmeta() {
        Rad rad = new Rad();
        rad.setId(9L);
        rad.setStatus("PREDAT");
        when(predmeti.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(predajePredmet()));
        when(radovi.findByPredmetIdAndStatus(4L, "PREDAT")).thenReturn(List.of(rad));

        assertEquals(9L, service.predati(4L, profesor).getFirst().getId());
        verify(radovi).findByPredmetIdAndStatus(4L, "PREDAT");
    }

    @Test
    void profesorNeVidiRadoveTudjegPredmeta() {
        assertStatus(HttpStatus.FORBIDDEN, () -> service.predati(4L, profesor));
        verify(radovi, never()).findByPredmetIdAndStatus(any(), any());
    }

    @Test
    void profesorOcenjujePredatRad() {
        Rad rad = new Rad();
        rad.setZadatak(zadatakSaPredmetom());
        rad.setStatus("PREDAT");
        OcenaRadaRequest request = new OcenaRadaRequest();
        request.setPoeni(8.5);
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(predmeti.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(predajePredmet()));
        when(radovi.save(rad)).thenReturn(rad);

        var odgovor = service.oceni(9L, request, profesor);

        assertEquals(8.5, rad.getPoeni());
        assertEquals("OCENJEN", rad.getStatus());
        assertEquals("OCENJEN", odgovor.getStatus());
        verify(radovi).save(rad);
    }

    @Test
    void ocenjivanjeOdbijaTudjegProfesoraPogresanStatusINevaljanePoene() {
        Rad rad = new Rad();
        rad.setZadatak(zadatakSaPredmetom());
        rad.setStatus("PREDAT");
        OcenaRadaRequest request = new OcenaRadaRequest();
        request.setPoeni(5.0);
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.oceni(9L, request, profesor));

        when(predmeti.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(predajePredmet()));
        rad.setStatus("PREUZET");
        assertStatus(HttpStatus.CONFLICT, () -> service.oceni(9L, request, profesor));

        rad.setStatus("PREDAT");
        request.setPoeni(-1.0);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.oceni(9L, request, profesor));
        request.setPoeni(Double.NaN);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.oceni(9L, request, profesor));
        verify(radovi, never()).save(any(Rad.class));
    }

    @Test
    void mojVracaRadovePrijavljenogStudenta() {
        Student student = student();
        student.setIme("Ana");
        Zadatak zadatak = zadatakSaPredmetom();
        zadatak.setNaziv("Prvi zadatak");
        zadatak.setRepoPutanja("https://github.com/predmet/zadatak");
        zadatak.getGrupa().getTermin().setVremePocetak(LocalTime.of(10, 0));
        zadatak.getGrupa().getTermin().setVremeKraj(LocalTime.of(12, 0));
        Rad rad = new Rad();
        rad.setId(7L);
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setVremePreuzimanja(LocalDateTime.of(2026, 10, 27, 12, 0));
        rad.setVremePredaje(LocalDateTime.of(2026, 10, 27, 14, 0));
        rad.setRepoPutanjaResenja("https://github.com/student/resenje");
        rad.setPoeni(8.5);
        rad.setStatus("OCENJEN");
        when(radovi.getRadByStudentEmail(auth.getName())).thenReturn(List.of(rad));

        var odgovor = service.moj(auth).getFirst();

        assertEquals(7L, odgovor.getId());
        assertEquals("Ana", odgovor.getStudent().getIme());
        assertEquals("Prvi zadatak", odgovor.getZadatak().getNaziv());
        assertEquals(rad.getVremePreuzimanja(), odgovor.getVremePreuzimanja());
        assertEquals(rad.getVremePredaje(), odgovor.getVremePredaje());
        assertEquals("https://github.com/student/resenje", odgovor.getRepoPutanjaResenja());
        assertEquals(8.5, odgovor.getPoeni());
        assertEquals("OCENJEN", odgovor.getStatus());
        assertEquals("https://github.com/predmet/zadatak", odgovor.getZadatak().getRepoPutanja());
        var termin = odgovor.getZadatak().getGrupa().getTermin();
        assertEquals(4L, termin.getPredmet().getId());
        assertEquals(LocalTime.of(10, 0), termin.getVremePocetak());
        assertEquals(LocalTime.of(12, 0), termin.getVremeKraj());
        verify(radovi).getRadByStudentEmail("student@example.com");
    }

    @Test
    void preuzmiCuvaRadZaPrijavljenogStudentaBezOdobrenja() {
        Student student = student();
        Zadatak zadatak = zadatak();
        when(studenti.pronadjiPoEmailu(auth.getName())).thenReturn(Optional.of(student));
        when(zadaci.findById(5L)).thenReturn(Optional.of(zadatak));
        when(prijave.existsByStudent_IdAndGrupa_Id(2L, 3L)).thenReturn(true);
        when(radovi.save(any(Rad.class))).thenAnswer(call -> {
            Rad rad = call.getArgument(0);
            rad.setId(9L);
            return rad;
        });

        assertEquals(9L, service.preuzmi(5L, auth));

        var sacuvanRad = ArgumentCaptor.forClass(Rad.class);
        verify(radovi).save(sacuvanRad.capture());
        assertSame(student, sacuvanRad.getValue().getStudent());
        assertSame(zadatak, sacuvanRad.getValue().getZadatak());
        assertEquals("PREUZET", sacuvanRad.getValue().getStatus());
        assertNotNull(sacuvanRad.getValue().getVremePreuzimanja());
        assertNull(sacuvanRad.getValue().getVremePredaje());
    }

    @Test
    void preuzmiOdbijaNeprijavljenogStudentaIDuplikat() {
        Student student = student();
        Zadatak zadatak = zadatak();
        assertStatus(HttpStatus.FORBIDDEN, () -> service.preuzmi(5L, auth));

        when(studenti.pronadjiPoEmailu(auth.getName())).thenReturn(Optional.of(student));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.preuzmi(5L, auth));

        zadatak.setGrupa(null);
        when(zadaci.findById(5L)).thenReturn(Optional.of(zadatak));
        assertStatus(HttpStatus.CONFLICT, () -> service.preuzmi(5L, auth));

        Grupa grupa = new Grupa();
        grupa.setId(3L);
        zadatak.setGrupa(grupa);
        assertStatus(HttpStatus.FORBIDDEN, () -> service.preuzmi(5L, auth));

        when(prijave.existsByStudent_IdAndGrupa_Id(2L, 3L)).thenReturn(true);
        when(radovi.existsByStudent_IdAndZadatak_Id(2L, 5L)).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.preuzmi(5L, auth));
        verify(radovi, never()).save(any(Rad.class));
    }

    @Test
    void predajCuvaResenjeSamoZaSopstveniPreuzetiRad() {
        Rad rad = new Rad();
        rad.setId(9L);
        rad.setStudent(student());
        rad.setStatus("PREUZET");
        PredajaRadaRequest request = new PredajaRadaRequest();
        request.setRepoPutanjaResenja("https://github.com/student/resenje");
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(radovi.save(rad)).thenReturn(rad);

        var odgovor = service.predaj(9L, request, auth);

        assertEquals("https://github.com/student/resenje", rad.getRepoPutanjaResenja());
        assertNotNull(rad.getVremePredaje());
        assertEquals("PREDAT", rad.getStatus());
        assertEquals("PREDAT", odgovor.getStatus());
        assertEquals(rad.getVremePredaje(), odgovor.getVremePredaje());
        verify(radovi).save(rad);
    }

    @Test
    void predajOdbijaTudjiIIzmenjeniRad() {
        PredajaRadaRequest request = new PredajaRadaRequest();
        request.setRepoPutanjaResenja("https://github.com/student/resenje");
        assertStatus(HttpStatus.NOT_FOUND, () -> service.predaj(9L, request, auth));

        Rad rad = new Rad();
        Student drugiStudent = student();
        drugiStudent.setEmail("drugi@example.com");
        rad.setStudent(drugiStudent);
        rad.setStatus("PREUZET");
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.predaj(9L, request, auth));

        rad.setStudent(student());
        rad.setStatus("PREDAT");
        assertStatus(HttpStatus.CONFLICT, () -> service.predaj(9L, request, auth));
        verify(radovi, never()).save(any(Rad.class));
    }

    @ParameterizedTest
    @MethodSource("neispravniPoeni")
    void neispravniPoeniNeMenjajuRad(Double poeni) {
        Rad rad = new Rad();
        rad.setZadatak(zadatakSaPredmetom());
        rad.setStatus("PREDAT");
        OcenaRadaRequest request = new OcenaRadaRequest();
        request.setPoeni(poeni);
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(predmeti.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(predajePredmet()));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.oceni(9L, request, profesor));
        assertEquals("PREDAT", rad.getStatus());
        assertNull(rad.getPoeni());
        verify(radovi, never()).save(any());
    }

    static Double[] neispravniPoeni() {
        return new Double[] {null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    }

    @Test
    void nulaPoenaJeDozvoljena() {
        Rad rad = new Rad();
        rad.setZadatak(zadatakSaPredmetom());
        rad.setStatus("PREDAT");
        OcenaRadaRequest request = new OcenaRadaRequest();
        request.setPoeni(0.0);
        when(radovi.findById(9L)).thenReturn(Optional.of(rad));
        when(predmeti.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(predajePredmet()));
        when(radovi.save(rad)).thenReturn(rad);

        assertEquals(0.0, service.oceni(9L, request, profesor).getPoeni());
        assertEquals("OCENJEN", rad.getStatus());
    }

    @Test
    void ocenaOdbijaNepostojeciRadINepotpuneVeze() {
        OcenaRadaRequest request = new OcenaRadaRequest();
        request.setPoeni(5.0);
        assertStatus(HttpStatus.NOT_FOUND, () -> service.oceni(9L, request, profesor));
        when(radovi.findById(9L)).thenReturn(Optional.of(new Rad()));
        assertStatus(HttpStatus.CONFLICT, () -> service.oceni(9L, request, profesor));
        verify(radovi, never()).save(any());
    }

    private static Student student() {
        Student student = new Student();
        student.setId(2L);
        student.setEmail("student@example.com");
        return student;
    }

    private static Zadatak zadatak() {
        Grupa grupa = new Grupa();
        grupa.setId(3L);
        Zadatak zadatak = new Zadatak();
        zadatak.setId(5L);
        zadatak.setGrupa(grupa);
        return zadatak;
    }

    private static Zadatak zadatakSaPredmetom() {
        Predmet predmet = new Predmet();
        predmet.setId(4L);
        Termin termin = new Termin();
        termin.setPredmet(predmet);
        Zadatak zadatak = zadatak();
        zadatak.getGrupa().setTermin(termin);
        return zadatak;
    }

    private static PredajePredmet predajePredmet() {
        PredajePredmet veza = new PredajePredmet();
        Predmet predmet = new Predmet();
        predmet.setId(4L);
        veza.setPredmet(predmet);
        return veza;
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
