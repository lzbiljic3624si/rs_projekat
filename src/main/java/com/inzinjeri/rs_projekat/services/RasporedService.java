package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.controllers.request.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RasporedService {
    private final RokRepository rokRepository;
    private final UcionicaRepository ucionicaRepository;
    private final TerminRepository terminRepository;
    private final GrupaRepository grupaRepository;
    private final PredmetRepository predmetRepository;
    private final PredajePredmetRepository predajePredmetRepository;

    @Transactional(readOnly = true)
    public List<Rok> rokovi() {
        return rokRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Ucionica> ucionice() {
        return ucionicaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Termin> termini(Long predmetId, Long rokId) {
        return terminRepository.pronadjiPoPredmetuIRoku(predmetId, rokId);
    }

    @Transactional(readOnly = true)
    public List<Grupa> grupe(Long terminId) {
        return grupaRepository.getGrupeZaTermin(terminId);
    }

    @Transactional
    public Long dodajRok(RokRequest request, Authentication auth) {
        proveriProfesora(auth);

        if (rokRepository.existsByNazivAndSkolskaGodina(
                request.getNaziv(), request.getSkolskaGodina())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Rok vec postoji"
            );
        }

        Rok rok = new Rok();
        rok.setNaziv(request.getNaziv());
        rok.setSkolskaGodina(request.getSkolskaGodina());

        return rokRepository.save(rok).getId();
    }

    @Transactional
    public Long dodajUcionicu(UcionicaRequest request, Authentication auth) {
        proveriProfesora(auth);

        if (ucionicaRepository.pronadjiPoNazivu(request.getNaziv()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ucionica vec postoji");
        }

        Ucionica ucionica = new Ucionica();
        ucionica.setNaziv(request.getNaziv());

        return ucionicaRepository.save(ucionica).getId();
    }

    @Transactional
    public Long dodajTermin(TerminRequest request, Authentication auth) {
        if (!request.getVremePocetak().isBefore(request.getVremeKraja())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pocetak termina mora biti pre kraja");
        }

        Predmet predmet = predmetRepository.findById(request.getPredmetId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Predmet ne postoji"
                ));

        proveriDaPredaje(auth, predmet.getId());

        Ucionica ucionica = ucionicaRepository.findById(
                        request.getUcionicaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ucionica ne postoji"));

        Rok rok = rokRepository.findById(request.getRokId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Rok ne postoji"));

        if (terminRepository.postojiPreklapanje(
                ucionica.getId(),
                request.getDatum(),
                request.getVremePocetak(),
                request.getVremeKraja())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Učionica je zauzeta u tom terminu");
        }

        Termin termin = new Termin();
        termin.setDatum(request.getDatum());
        termin.setVremePocetak(request.getVremePocetak());
        termin.setVremeKraj(request.getVremeKraja());
        termin.setUcionica(ucionica);
        termin.setPredmet(predmet);
        termin.setRok(rok);

        return terminRepository.save(termin).getId();
    }

    @Transactional
    public Long dodajGrupu(GrupaRequest request, Authentication auth) {
        Termin termin = terminRepository.findById(request.getTerminId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Termin ne postoji"));

        if (termin.getPredmet() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Termin nema predmet");
        }

        proveriDaPredaje(auth, termin.getPredmet().getId());

        if (grupaRepository.existsByNaziv(request.getNaziv())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Grupa već postoji");
        }

        Grupa grupa = new Grupa();
        grupa.setNaziv(request.getNaziv());
        grupa.setTermin(termin);

        return grupaRepository.save(grupa).getId();

    }

    private void proveriProfesora(Authentication auth) {
        boolean profesor = auth.getAuthorities().stream()
                .anyMatch(uloga ->
                        "ROLE_PROFESOR".equals(uloga.getAuthority()));

        if (!profesor) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Samo profesor može da menja raspored");
        }
    }

    private void proveriDaPredaje(
            Authentication auth, Long predmetId) {
        proveriProfesora(auth);

        boolean predaje = predajePredmetRepository
                .getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza ->
                        veza.getPredmet() != null
                                && predmetId.equals(
                                veza.getPredmet().getId()));

        if (!predaje) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Profesor ne predaje ovaj predmet");
        }
    }
}
