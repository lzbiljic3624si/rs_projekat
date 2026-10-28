package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RasporedControllerTest {
    private final RokRepository rokovi = mock(RokRepository.class);
    private final UcionicaRepository ucionice = mock(UcionicaRepository.class);
    private final TerminRepository termini = mock(TerminRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final PredmetRepository predmeti = mock(PredmetRepository.class);
    private final PredajePredmetRepository predaje = mock(PredajePredmetRepository.class);
    private final RasporedController controller = new RasporedController(rokovi, ucionice, termini, grupe, predmeti, predaje);
    private final Authentication profesor = auth("profesor@test.rs", "ROLE_PROFESOR");
    private final Authentication student = auth("student@test.rs", "ROLE_STUDENT");

    @Test
    void rasporedPrikazujeTermineZaPredmetIRok() {
        Termin termin = new Termin();
        termin.setId(7L);
        termin.setPredmet(predmet());
        termin.setRok(rok());
        termin.setUcionica(ucionica());
        termin.setDatum(LocalDate.of(2027, 6, 1));
        termin.setVremePocetak(LocalTime.of(10, 0));
        termin.setVremeKraj(LocalTime.of(12, 0));
        when(termini.pronadjiPoPredmetuIRoku(4L, 6L)).thenReturn(List.of(termin));

        var odgovor = controller.termini(4L, 6L).getFirst();
        assertEquals(7L, odgovor.getId());
        assertEquals(4L, odgovor.getPredmet().getId());
        assertEquals(6L, odgovor.getRok().getId());
        assertEquals(5L, odgovor.getUcionica().getId());
        assertEquals(LocalDate.of(2027, 6, 1), odgovor.getDatum());
        assertEquals(LocalTime.of(10, 0), odgovor.getVremePocetka());
        assertEquals(LocalTime.of(12, 0), odgovor.getVremeKraja());
    }

    @Test
    void profesorDodajeTerminSaIzabranimVezama() {
        TerminRequest request = zahtevZaTermin();
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet()));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(ucionice.findById(5L)).thenReturn(Optional.of(ucionica()));
        when(rokovi.findById(6L)).thenReturn(Optional.of(rok()));
        when(termini.save(any())).thenAnswer(call -> {
            Termin termin = call.getArgument(0);
            termin.setId(7L);
            return termin;
        });

        assertEquals(7L, controller.dodajTermin(request, profesor));
        verify(termini).save(argThat(termin -> termin.getPredmet().getId().equals(4L)
                && termin.getRok().getId().equals(6L) && termin.getUcionica().getId().equals(5L)
                && termin.getVremePocetak().equals(LocalTime.of(10, 0))));
    }

    @Test
    void terminOdbijaLoseVremeZauzetuUcionicuITudjegProfesora() {
        TerminRequest request = zahtevZaTermin();
        request.setVremeKraja(LocalTime.of(10, 0));
        assertStatus(HttpStatus.BAD_REQUEST, () -> controller.dodajTermin(request, profesor));
        request.setVremeKraja(LocalTime.of(12, 0));
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet()));
        assertStatus(HttpStatus.FORBIDDEN, () -> controller.dodajTermin(request, profesor));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(ucionice.findById(5L)).thenReturn(Optional.of(ucionica()));
        when(rokovi.findById(6L)).thenReturn(Optional.of(rok()));
        when(termini.postojiPreklapanje(5L, request.getDatum(), request.getVremePocetak(), request.getVremeKraja())).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> controller.dodajTermin(request, profesor));
        verify(termini, never()).save(any());
    }

    @Test
    void profesorDodajeGrupuSvomTerminu() {
        Termin termin = new Termin();
        termin.setPredmet(predmet());
        GrupaRequest request = new GrupaRequest();
        request.setNaziv("G1");
        request.setTerminId(7L);
        when(termini.findById(7L)).thenReturn(Optional.of(termin));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(grupe.save(any())).thenAnswer(call -> {
            Grupa grupa = call.getArgument(0);
            grupa.setId(3L);
            return grupa;
        });

        assertEquals(3L, controller.dodajGrupu(request, profesor));
        verify(grupe).save(argThat(grupa -> grupa.getTermin() == termin && "G1".equals(grupa.getNaziv())));
    }

    @Test
    void studentNeMozeDaMenjaRaspored() {
        RokRequest request = new RokRequest();
        request.setNaziv("Junski");
        request.setSkolskaGodina(2026);
        assertStatus(HttpStatus.FORBIDDEN, () -> controller.dodajRok(request, student));
        verify(rokovi, never()).save(any());
    }

    private static TerminRequest zahtevZaTermin() {
        TerminRequest request = new TerminRequest();
        request.setDatum(LocalDate.of(2027, 6, 1));
        request.setVremePocetak(LocalTime.of(10, 0));
        request.setVremeKraja(LocalTime.of(12, 0));
        request.setPredmetId(4L);
        request.setUcionicaId(5L);
        request.setRokId(6L);
        return request;
    }

    private static Predmet predmet() {
        Predmet predmet = new Predmet();
        predmet.setId(4L);
        return predmet;
    }

    private static Ucionica ucionica() {
        Ucionica ucionica = new Ucionica();
        ucionica.setId(5L);
        return ucionica;
    }

    private static Rok rok() {
        Rok rok = new Rok();
        rok.setId(6L);
        return rok;
    }

    private static PredajePredmet veza() {
        PredajePredmet veza = new PredajePredmet();
        veza.setPredmet(predmet());
        return veza;
    }

    private static Authentication auth(String email, String uloga) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(uloga)));
    }

    private static void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }
}
