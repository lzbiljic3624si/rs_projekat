package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.PredajePredmetRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredajePredmetServiceTest {
    private final PredajePredmetRepository veze = mock(PredajePredmetRepository.class);
    private final ProfesorRepository profesori = mock(ProfesorRepository.class);
    private final PredmetRepository predmeti = mock(PredmetRepository.class);
    private final PredajePredmetService service = new PredajePredmetService(veze, profesori, predmeti);
    private final PredajePredmetRequest request = new PredajePredmetRequest();
    private final Profesor profesor = new Profesor();
    private final Predmet predmet = new Predmet();

    @BeforeEach
    void podaci() {
        profesor.setId(2L);
        profesor.setEmail("mapetrovic@raf.rs");
        predmet.setId(3L);
        request.setEmailProfesora("MAPetrovic@raf.rs");
        request.setNazivPredmeta("Razvoj softvera");
        when(profesori.findByEmailIgnoreCase("MAPetrovic@raf.rs")).thenReturn(Optional.of(profesor));
        when(predmeti.getPredmetByNaziv("Razvoj softvera")).thenReturn(List.of(predmet));
        when(veze.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void dodeljujePostojecegProfesoraPostojecemPredmetu() {
        PredajePredmet veza = service.dodaj(request);
        assertSame(profesor, veza.getProfesor());
        assertSame(predmet, veza.getPredmet());
    }

    @Test
    void duplikatVezeSeOdbija() {
        when(veze.existsByProfesor_IdAndPredmet_Id(2L, 3L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.dodaj(request));
        verify(veze, never()).saveAndFlush(any());
    }

    @Test
    void nepostojeciProfesorSeOdbija() {
        when(profesori.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.dodaj(request));
        verify(veze, never()).saveAndFlush(any());
    }

    @Test
    void nepostojeciPredmetSeOdbija() {
        when(predmeti.getPredmetByNaziv(anyString())).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> service.dodaj(request));
        verify(veze, never()).saveAndFlush(any());
    }

    @Test
    void konkurentniDuplikatVezeVraca409() {
        when(veze.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplikat"));
        assertThrows(ConflictException.class, () -> service.dodaj(request));
    }
}
