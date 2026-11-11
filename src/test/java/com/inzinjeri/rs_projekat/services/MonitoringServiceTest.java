package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.repositories.TerminRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringServiceTest {
    private final TerminRepository termini = mock(TerminRepository.class);
    private final PrijavljenStudentRepository prijave = mock(PrijavljenStudentRepository.class);
    private final RadRepository radovi = mock(RadRepository.class);
    private final MonitoringService service = new MonitoringService(termini, prijave, radovi);

    @Test
    void prikazujePrijavljeneStatuseIZbirneBrojeve() {
        when(termini.findById(7L)).thenReturn(Optional.of(new Termin()));
        Student ceka = student(1L);
        Student radi = student(2L);
        Student predao = student(3L);
        Student pregledan = student(4L);
        when(prijave.findByGrupa_Termin_Id(7L)).thenReturn(List.of(
                prijava(ceka), prijava(radi), prijava(predao), prijava(pregledan), prijava(pregledan)));
        when(radovi.findByZadatak_Grupa_Termin_Id(7L)).thenReturn(List.of(
                rad(radi, StatusRada.PREUZET), rad(predao, StatusRada.PREDAT),
                rad(pregledan, StatusRada.OCENJEN), rad(pregledan, StatusRada.PREUZET),
                rad(student(5L), StatusRada.PREDAT)));

        var odgovor = service.zaTermin(7L);

        assertEquals(4, odgovor.ukupnoPrijavljenih());
        assertEquals(3, odgovor.preuzeli());
        assertEquals(2, odgovor.predali());
        assertEquals(1, odgovor.josRade());
        assertEquals(1, odgovor.pregledani());
        assertNull(odgovor.studenti().getFirst().status());
        assertEquals(StatusRada.OCENJEN, odgovor.studenti().getLast().status());
    }

    @Test
    void nepostojeciTerminVracaGresku() {
        assertThrows(ResourceNotFoundException.class, () -> service.zaTermin(99L));
        verifyNoInteractions(prijave, radovi);
    }

    private Student student(Long id) {
        Student student = new Student();
        student.setId(id);
        return student;
    }

    private PrijavljenStudent prijava(Student student) {
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setStudent(student);
        return prijava;
    }

    private Rad rad(Student student, StatusRada status) {
        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setStatus(status);
        return rad;
    }
}
