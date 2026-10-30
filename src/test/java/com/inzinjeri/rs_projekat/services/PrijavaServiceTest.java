package com.inzinjeri.rs_projekat.services;

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

class PrijavaServiceTest {
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final PredajePredmetRepository predmeti = mock(PredajePredmetRepository.class);
    private final PrijavaService service = new PrijavaService(prijave, studenti, grupe, predmeti);
    private final Authentication studentAuth = auth("student@test.rs", "ROLE_STUDENT");
    private final Authentication profesorAuth = auth("profesor@test.rs", "ROLE_PROFESOR");

    @Test
    void studentSeDirektnoPrijavljujeUGrupu() {
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

        assertEquals(8L, service.prijaviSe(3L, studentAuth));
        verify(prijave).save(argThat(prijava -> prijava.getStudent() == student
                && prijava.getGrupa() == grupa));
    }

    @Test
    void ponovljenaPrijavaSeOdbija() {
        Student student = new Student();
        student.setId(2L);
        when(studenti.pronadjiPoEmailu(studentAuth.getName())).thenReturn(Optional.of(student));
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        when(prijave.existsByStudent_IdAndGrupa_Id(2L, 3L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.prijaviSe(3L, studentAuth));
        verify(prijave, never()).save(any());
    }

    @Test
    void profesorVidiPrijaveZaSvojuGrupu() {
        Grupa grupa = grupa();
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setId(8L);
        prijava.setGrupa(grupa);
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        when(predmeti.getPredmetiZaPredavaca(profesorAuth.getName())).thenReturn(List.of(veza()));
        when(prijave.findByGrupa_Id(3L)).thenReturn(List.of(prijava));

        assertEquals(8L, service.prijavaZaGrupu(3L, profesorAuth).getFirst().getId());
    }

    @Test
    void mojePrijaveSuFiltriranePoEmailuStudenta() {
        Student student = new Student();
        PrijavljenStudent prijava = new PrijavljenStudent();
        when(studenti.pronadjiPoEmailu(studentAuth.getName())).thenReturn(Optional.of(student));
        when(prijave.getStudentByEmail(studentAuth.getName())).thenReturn(List.of(prijava));

        assertEquals(List.of(prijava), service.moje(studentAuth));
        verify(prijave).getStudentByEmail("student@test.rs");
        assertStatus(HttpStatus.FORBIDDEN, () -> service.moje(profesorAuth));
        verify(prijave, never()).getStudentByEmail(profesorAuth.getName());
    }

    @Test
    void nepostojeciPodaciINepotpunaGrupaSeOdbijaju() {
        assertStatus(HttpStatus.FORBIDDEN, () -> service.prijaviSe(3L, studentAuth));
        when(studenti.pronadjiPoEmailu(studentAuth.getName())).thenReturn(Optional.of(new Student()));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.prijaviSe(3L, studentAuth));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.prijavaZaGrupu(3L, profesorAuth));
        Grupa grupa = grupa();
        grupa.setTermin(null);
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa));
        assertStatus(HttpStatus.CONFLICT, () -> service.prijaviSe(3L, studentAuth));
        verify(prijave, never()).save(any());
    }

    @Test
    void tudjiProfesorNeVidiPrijaveGrupe() {
        when(grupe.findById(3L)).thenReturn(Optional.of(grupa()));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.prijavaZaGrupu(3L, profesorAuth));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.prijavaZaGrupu(3L, studentAuth));
        verify(prijave, never()).findByGrupa_Id(any());
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
