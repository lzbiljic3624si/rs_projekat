package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RasporedServiceTest {
    private final RokRepository rokovi = mock(RokRepository.class);
    private final UcionicaRepository ucionice = mock(UcionicaRepository.class);
    private final TerminRepository termini = mock(TerminRepository.class);
    private final GrupaRepository grupe = mock(GrupaRepository.class);
    private final PredmetRepository predmeti = mock(PredmetRepository.class);
    private final PredajePredmetRepository predaje = mock(PredajePredmetRepository.class);
    private final RasporedService service = new RasporedService(rokovi, ucionice, termini, grupe, predmeti, predaje);
    private final Authentication profesor = auth("profesor@test.rs", "ROLE_PROFESOR");
    private final Authentication student = auth("student@test.rs", "ROLE_STUDENT");

    @Test
    void rasporedPrikazujeTermineZaPredmetIRok() {
        Termin termin = new Termin();
        termin.setId(7L);
        termin.setPredmet(predmet());
        termin.setRok(rok());
        termin.setUcionica(ucionica());
        termin.setDatum(LocalDate.now().plusDays(1));
        termin.setVremePocetak(LocalTime.of(10, 0));
        termin.setVremeKraj(LocalTime.of(12, 0));
        when(termini.pronadjiPoPredmetuIRoku(4L, 6L)).thenReturn(List.of(termin));

        var odgovor = service.termini(4L, 6L).getFirst();
        assertEquals(7L, odgovor.getId());
        assertEquals(4L, odgovor.getPredmet().getId());
        assertEquals(6L, odgovor.getRok().getId());
        assertEquals(5L, odgovor.getUcionica().getId());
        assertEquals(LocalDate.now().plusDays(1), odgovor.getDatum());
        assertEquals(LocalTime.of(10, 0), odgovor.getVremePocetak());
        assertEquals(LocalTime.of(12, 0), odgovor.getVremeKraj());
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

        assertEquals(7L, service.dodajTermin(request, profesor));
        verify(termini).save(argThat(termin -> termin.getPredmet().getId().equals(4L)
                && termin.getRok().getId().equals(6L) && termin.getUcionica().getId().equals(5L)
                && termin.getVremePocetak().equals(LocalTime.of(10, 0))));
    }

    @Test
    void terminOdbijaLoseVremeZauzetuUcionicuITudjegProfesora() {
        TerminRequest request = zahtevZaTermin();
        request.setVremeKraja(LocalTime.of(10, 0));
        assertThrows(BadRequestException.class, () -> service.dodajTermin(request, profesor));
        request.setVremeKraja(LocalTime.of(12, 0));
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet()));
        assertThrows(ForbiddenException.class, () -> service.dodajTermin(request, profesor));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(ucionice.findById(5L)).thenReturn(Optional.of(ucionica()));
        when(rokovi.findById(6L)).thenReturn(Optional.of(rok()));
        when(termini.postojiPreklapanje(5L, request.getDatum(), request.getVremePocetak(), request.getVremeKraja())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.dodajTermin(request, profesor));
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

        assertEquals(3L, service.dodajGrupu(request, profesor));
        verify(grupe).save(argThat(grupa -> grupa.getTermin() == termin && "G1".equals(grupa.getNaziv())));
    }

    @Test
    void studentNeMozeDaMenjaRaspored() {
        RokRequest request = new RokRequest();
        request.setNaziv("Junski");
        request.setSkolskaGodina(2026);
        assertThrows(ForbiddenException.class, () -> service.dodajRok(request, student));
        verify(rokovi, never()).save(any());
    }

    @Test
    void rokIUcionicaSeCuvajuSaSvimPoljima() {
        RokRequest rok = new RokRequest();
        rok.setNaziv("Junski");
        rok.setSkolskaGodina(2026);
        when(rokovi.save(any())).thenAnswer(call -> {
            Rok sacuvan = call.getArgument(0); sacuvan.setId(6L); return sacuvan;
        });
        UcionicaRequest ucionica = new UcionicaRequest();
        ucionica.setNaziv("Lab 1");
        when(ucionice.save(any())).thenAnswer(call -> {
            Ucionica sacuvana = call.getArgument(0); sacuvana.setId(5L); return sacuvana;
        });

        assertEquals(6L, service.dodajRok(rok, profesor));
        assertEquals(5L, service.dodajUcionicu(ucionica, profesor));
        verify(rokovi).save(argThat(r -> "Junski".equals(r.getNaziv()) && Integer.valueOf(2026).equals(r.getSkolskaGodina())));
        verify(ucionice).save(argThat(u -> "Lab 1".equals(u.getNaziv())));
    }

    @Test
    void duplikatiRokaUcioniceIGrupeSeNeCuvaju() {
        RokRequest rok = new RokRequest(); rok.setNaziv("Junski"); rok.setSkolskaGodina(2026);
        when(rokovi.existsByNazivAndSkolskaGodina("Junski", 2026)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.dodajRok(rok, profesor));
        UcionicaRequest ucionica = new UcionicaRequest(); ucionica.setNaziv("Lab 1");
        when(ucionice.pronadjiPoNazivu("Lab 1")).thenReturn(Optional.of(ucionica()));
        assertThrows(ConflictException.class, () -> service.dodajUcionicu(ucionica, profesor));
        Termin termin = new Termin(); termin.setPredmet(predmet());
        GrupaRequest grupa = new GrupaRequest(); grupa.setNaziv("G1"); grupa.setTerminId(7L);
        when(termini.findById(7L)).thenReturn(Optional.of(termin));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        when(grupe.existsByNaziv("G1")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.dodajGrupu(grupa, profesor));
        verify(rokovi, never()).save(any());
        verify(ucionice, never()).save(any());
        verify(grupe, never()).save(any());
    }

    @Test
    void terminProveravaSveObavezneVeze() {
        TerminRequest request = zahtevZaTermin();
        assertThrows(ResourceNotFoundException.class, () -> service.dodajTermin(request, profesor));
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet()));
        when(predaje.getPredmetiZaPredavaca(profesor.getName())).thenReturn(List.of(veza()));
        assertThrows(ResourceNotFoundException.class, () -> service.dodajTermin(request, profesor));
        when(ucionice.findById(5L)).thenReturn(Optional.of(ucionica()));
        assertThrows(ResourceNotFoundException.class, () -> service.dodajTermin(request, profesor));
        verify(termini, never()).save(any());
    }

    @Test
    void grupaProveravaTerminIPredmet() {
        GrupaRequest request = new GrupaRequest(); request.setNaziv("G1"); request.setTerminId(7L);
        assertThrows(ResourceNotFoundException.class, () -> service.dodajGrupu(request, profesor));
        when(termini.findById(7L)).thenReturn(Optional.of(new Termin()));
        assertThrows(ConflictException.class, () -> service.dodajGrupu(request, profesor));
        verify(grupe, never()).save(any());
    }

    @Test
    void studentNeMozeDaKreiraUcionicuTerminNiGrupu() {
        assertThrows(ForbiddenException.class, () -> service.dodajUcionicu(new UcionicaRequest(), student));
        when(predmeti.findById(4L)).thenReturn(Optional.of(predmet()));
        assertThrows(ForbiddenException.class, () -> service.dodajTermin(zahtevZaTermin(), student));
        Termin termin = new Termin(); termin.setPredmet(predmet());
        when(termini.findById(7L)).thenReturn(Optional.of(termin));
        GrupaRequest grupa = new GrupaRequest(); grupa.setTerminId(7L);
        assertThrows(ForbiddenException.class, () -> service.dodajGrupu(grupa, student));
        verify(ucionice, never()).save(any());
        verify(termini, never()).save(any());
        verify(grupe, never()).save(any());
    }

    @Test
    void listeKoristeTacneRepozitorijumeIFiltere() {
        when(rokovi.findAll()).thenReturn(List.of(rok()));
        when(ucionice.findAll()).thenReturn(List.of(ucionica()));
        Grupa grupa = new Grupa(); grupa.setId(3L);
        when(grupe.getGrupeZaTermin(7L)).thenReturn(List.of(grupa));
        assertEquals(6L, service.rokovi().getFirst().getId());
        assertEquals(5L, service.ucionice().getFirst().getId());
        assertEquals(List.of(grupa), service.grupe(7L));
        verify(grupe).getGrupeZaTermin(7L);
    }

    private static TerminRequest zahtevZaTermin() {
        TerminRequest request = new TerminRequest();
        request.setDatum(LocalDate.now().plusDays(1));
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

}
