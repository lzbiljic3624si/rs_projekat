package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredmetServiceTest {
    private final PredmetRepository repository = mock(PredmetRepository.class);
    private final PredmetService service = new PredmetService(repository);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "0", " 0 "})
    void predmetBezUslovaCuvaNull(String uslov) {
        Predmet predmet = new Predmet();
        predmet.setNaziv("Uvod u programiranje");
        predmet.setUslov(uslov);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertNull(service.save(predmet).getUslov());
    }

    @Test
    void cuvaNovPredmet() {
        Predmet predmet = new Predmet();
        predmet.setNaziv("Razvoj softvera");
        when(repository.save(predmet)).thenReturn(predmet);

        assertSame(predmet, service.save(predmet));
        verify(repository).getPredmetByNaziv("Razvoj softvera");
        verify(repository).save(predmet);
    }

    @Test
    void duplikatSeNeCuva() {
        Predmet predmet = new Predmet();
        predmet.setNaziv("Razvoj softvera");
        when(repository.getPredmetByNaziv(predmet.getNaziv())).thenReturn(List.of(predmet));

        assertThrows(ConflictException.class, () -> service.save(predmet));
        verify(repository, never()).save(any());
    }

    @Test
    void citaPredmeteIPrepoznajeNepostojeciId() {
        Predmet predmet = new Predmet();
        when(repository.findAll()).thenReturn(List.of(predmet));
        when(repository.findById(1L)).thenReturn(Optional.of(predmet));

        assertEquals(List.of(predmet), service.findAll());
        assertSame(predmet, service.findById(1L));
        assertThrows(ResourceNotFoundException.class, () -> service.findById(2L));
    }
}
