package com.inzinjeri.rs_projekat.controllers;

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

class PrijavaControllerTest {
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final PredajePredmetRepository predmeti = mock(PredajePredmetRepository.class);
    private final PrijavaController controller = new PrijavaController(prijave, studenti, grupe, predmeti);
    private final Authentication studentAuth = auth("student@test.rs", "ROLE_STUDENT");
    private final Authentication profesorAuth = auth("profesor@test.rs", "ROLE_PROFESOR");

    @Test
    void prijavaKreiraZahtevNaCekanju() {
        Student student = new Student();
        student.setId(2L);
        Grupa grupa = grupa();
        when(studenti.pronadjiPoEmailu(studentAuth.getName())).thenReturn(Optional.of(student));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(prijave.save(any())).thenAnswer(call -> {
            PrijavljenStudent prijava = call.getArgument(0);
            prijava.setId(8L);
            return prijava;
        });

        assertEquals(8L, controller.prijaviSe(3L, studentAuth));
        verify(prijave).save(argThat(prijava -> prijava.getStudent() == student
                && prijava.getGrupa() == grupa && "NA_CEKANJU".equals(prijava.getStatus())));
    }

    @Test
    void ponovljenaPrijavaSeOdbija() {
        Student student = new Student();
        student.setId(2L);
        when(studenti.pronadjiPoEmailu(studentAuth.getName())).thenReturn(Optional.of(student));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        when(prijave.existsByStudent_IdAndGrupa_Id(2L, 3L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> controller.prijaviSe(3L, studentAuth));
        verify(prijave, never()).save(any());
    }

    @Test
    void profesorPredmetaPrihvataPrijavu() {
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setId(8L);
        prijava.setGrupa(grupa());
        prijava.setStatus("NA_CEKANJU");
        when(prijave.findById(8L)).thenReturn(Optional.of(prijava));
        when(predmeti.getPredmetiZaPredavaca(profesorAuth.getName())).thenReturn(List.of(veza()));
        when(prijave.save(prijava)).thenReturn(prijava);

        assertEquals("PRIHVACEN", controller.prihvati(8L, profesorAuth).getStatus());
        assertEquals("PRIHVACEN", prijava.getStatus());
        verify(prijave).save(prijava);
    }

    @Test
    void tudjiProfesorIStudentNeMoguDaPrihvatePrijavu() {
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setGrupa(grupa());
        prijava.setStatus("NA_CEKANJU");
        when(prijave.findById(8L)).thenReturn(Optional.of(prijava));
        assertStatus(HttpStatus.FORBIDDEN, () -> controller.prihvati(8L, profesorAuth));
        when(predmeti.getPredmetiZaPredavaca(studentAuth.getName())).thenReturn(List.of(veza()));
        assertStatus(HttpStatus.FORBIDDEN, () -> controller.prihvati(8L, studentAuth));
        verify(prijave, never()).save(any());
    }

    @Test
    void profesorVidiPrijaveZaSvojuGrupu() {
        Grupa grupa = grupa();
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setId(8L);
        prijava.setGrupa(grupa);
        prijava.setStatus("NA_CEKANJU");
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(predmeti.getPredmetiZaPredavaca(profesorAuth.getName())).thenReturn(List.of(veza()));
        when(prijave.findByGrupa_Id(3L)).thenReturn(List.of(prijava));

        assertEquals(8L, controller.prijaveZaGrupu(3L, profesorAuth).getFirst().getId());
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
