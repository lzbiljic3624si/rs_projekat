package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.PredmetRequest;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredmetControllerTest {

    @Test
    void addsAndReadsPredmetAndRejectsDuplicateName() {
        var repository = mock(PredmetRepository.class);
        var controller = new PredmetController(repository);
        var request = new PredmetRequest();
        request.setNaziv("Razvoj softvera");
        request.setJezik("Java");
        request.setUslov("Nema");

        when(repository.getPredmetByNaziv(request.getNaziv())).thenReturn(List.of());
        when(repository.save(any(Predmet.class))).thenAnswer(invocation -> {
            Predmet predmet = invocation.getArgument(0);
            predmet.setId(1L);
            return predmet;
        });
        assertEquals(1L, controller.add(request));

        var predmet = new Predmet();
        predmet.setId(1L);
        predmet.setNaziv(request.getNaziv());
        predmet.setJezik(request.getJezik());
        predmet.setUslov(request.getUslov());
        when(repository.findAll()).thenReturn(List.of(predmet));
        when(repository.findById(1L)).thenReturn(Optional.of(predmet));
        assertEquals("Razvoj softvera", controller.all().getFirst().getNaziv());
        assertEquals("Java", controller.byId(1L).getJezik());

        when(repository.getPredmetByNaziv(request.getNaziv())).thenReturn(List.of(predmet));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class, () -> controller.add(request)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> controller.byId(2L)).getStatusCode());
    }
}
