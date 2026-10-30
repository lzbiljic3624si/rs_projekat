package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZadatakServiceTest {
    private final ZadatakRepository zadaci = mock(ZadatakRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final PredajePredmetRepository predaje = mock(PredajePredmetRepository.class);
    private final ZadatakService service = new ZadatakService(zadaci, grupe, studenti, prijave, predaje);
    private final Authentication student = auth("student@test.rs", "ROLE_STUDENT");
    private final Authentication profesor = auth("profesor@test.rs", "ROLE_PROFESOR");

    @Test
    void prihvacenStudentVidiZadatkeGrupe() {
        Grupa grupa = grupa();
        Student studentModel = new Student();
        studentModel.setId(2L);
        Zadatak zadatak = new Zadatak();
        zadatak.setId(9L);
        zadatak.setNaziv("Prvi zadatak");
        zadatak.setGrupa(grupa);
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(studenti.pronadjiPoEmailu(student.getName())).thenReturn(Optional.of(studentModel));
        when(prijave.existsByStudent_IdAndGrupa_IdAndStatus(2L, 3L, "PRIHVACEN")).thenReturn(true);
        when(zadaci.pronadjiPoGrupi(3L)).thenReturn(Optional.of(zadatak));

        var odgovor = service.zaGrupu(3L, student);
        assertEquals(9L, odgovor.getId());
        assertEquals("Prvi zadatak", odgovor.getNaziv());
        assertEquals(3L, odgovor.getGrupa().getId());
    }

    @Test
    void studentBezPrihvacenePrijaveNeVidiZadatke() {
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.zaGrupu(3L, student));
        verify(zadaci, never()).pronadjiPoGrupi(any());
    }

    @Test
    void profesorPredmetaDodajeZadatak() {
        Grupa grupa = grupa();
        ZadatakRequest request = request();
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(zadaci.save(any())).thenAnswer(call -> {
            Zadatak zadatak = call.getArgument(0);
            zadatak.setId(9L);
            return zadatak;
        });

        assertEquals(9L, service.dodaj(request, profesor));
        verify(zadaci).save(argThat(zadatak -> zadatak.getGrupa() == grupa
                && "Prvi zadatak".equals(zadatak.getNaziv())
                && "https://github.com/test/zadatak".equals(zadatak.getRepoPutanja())));
    }

    @Test
    void tudjiProfesorIStudentNeMoguDaDodajuZadatak() {
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.dodaj(request(), profesor));
        when(predaje.getPredmetiZaPredavaca(student.getName())).thenReturn(List.of(veza()));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.dodaj(request(), student));
        verify(zadaci, never()).save(any());
    }

    @Test
    void duplikatPutanjeZadatkaSeOdbija() {
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(zadaci.existsByRepoPutanja("https://github.com/test/zadatak")).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.dodaj(request(), profesor));
        verify(zadaci, never()).save(any());
    }

    @Test
    void grupaBezZadatkaVraca404() {
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.zaGrupu(3L, profesor));
    }

    @Test
    void grupaMozeDaImaSamoJedanZadatak() {
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(zadaci.existsByGrupa_Id(3L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.dodaj(request(), profesor));
        verify(zadaci, never()).save(any());
    }

    @Test
    void nepostojecaGrupaINepotpunaVezaSeOdbijaju() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.zaGrupu(3L, profesor));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.dodaj(request(), profesor));
        Grupa grupa = grupa();
        grupa.setTermin(null);
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        assertStatus(HttpStatus.CONFLICT, () -> service.zaGrupu(3L, profesor));
        assertStatus(HttpStatus.CONFLICT, () -> service.dodaj(request(), profesor));
        verify(zadaci, never()).save(any());
    }

    @Test
    void postojeciStudentBezPrihvacenePrijaveNemaPristup() {
        Student studentModel = new Student();
        studentModel.setId(2L);
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        when(studenti.pronadjiPoEmailu(student.getName())).thenReturn(Optional.of(studentModel));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.zaGrupu(3L, student));
        verify(zadaci, never()).pronadjiPoGrupi(any());
    }

    private static ZadatakRequest request() {
        ZadatakRequest request = new ZadatakRequest();
        request.setNaziv("Prvi zadatak");
        request.setGrupaId(3L);
        request.setRepoPutanja("https://github.com/test/zadatak");
        return request;
    }

    private static Grupa grupa() {
        Predmet predmet = new Predmet();
        predmet.setId(4L);
        Termin termin = new Termin();
        termin.setPredmet(predmet);
        Grupa grupa = new Grupa();
        grupa.setId(3L);
        grupa.setTermin(termin);
        return grupa;
    }

    private static PredajePredmet veza() {
        PredajePredmet veza = new PredajePredmet();
        veza.setPredmet(grupa().getTermin().getPredmet());
        return veza;
    }

    private static Authentication auth(String email, String uloga) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(uloga)));
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
