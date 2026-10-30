package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredmetServiceTest {
    private final PredmetRepository repository = mock(PredmetRepository.class);
    private final PredmetService service = new PredmetService(repository);

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

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class, () -> service.save(predmet)).getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void citaPredmeteIPrepoznajeNepostojeciId() {
        Predmet predmet = new Predmet();
        when(repository.findAll()).thenReturn(List.of(predmet));
        when(repository.findById(1L)).thenReturn(Optional.of(predmet));

        assertEquals(List.of(predmet), service.findAll());
        assertSame(predmet, service.findById(1L));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service.findById(2L)).getStatusCode());
    }
}
