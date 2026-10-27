package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.GrupaRequest;
import com.inzinjeri.rs_projekat.controllers.request.RokRequest;
import com.inzinjeri.rs_projekat.controllers.request.TerminRequest;
import com.inzinjeri.rs_projekat.controllers.request.UcionicaRequest;
import com.inzinjeri.rs_projekat.controllers.response.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/raspored")
@RequiredArgsConstructor
public class RasporedController {

    private final RokRepository rokovi;
    private final UcionicaRepository ucionice;
    private final TerminRepository termini;
    private final GrupaRepository grupe;
    private final PredmetRepository predmeti;
    private final PredajePredmetRepository predajePredmet;

    @GetMapping("/rokovi")
    public List<RokResponse> rokovi() {
        return rokovi.findAll().stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/ucionice")
    public List<UcionicaResponse> ucionice() {
        return ucionice.findAll().stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/termini")
    public List<TerminResponse> termini(@RequestParam Long predmetId, @RequestParam Long rokId) {
        return termini.pronadjiPoPredmetuIRoku(predmetId, rokId).stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/grupe")
    public List<GrupaResponse> grupe(@RequestParam Long terminId) {
        return grupe.getGrupeZaTermin(terminId).stream().map(EntityMappers::toResponse).toList();
    }

    @PostMapping("/rokovi")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajRok(@Valid @RequestBody RokRequest request, Authentication auth) {
        proveriProfesora(auth);
        if (rokovi.existsByNazivAndSkolskaGodina(request.getNaziv(), request.getSkolskaGodina())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rok već postoji");
        }
        Rok rok = new Rok();
        rok.setNaziv(request.getNaziv());
        rok.setSkolskaGodina(request.getSkolskaGodina());
        return rokovi.save(rok).getId();
    }

    @PostMapping("/ucionice")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajUcionicu(@Valid @RequestBody UcionicaRequest request, Authentication auth) {
        proveriProfesora(auth);
        if (ucionice.pronadjiPoNazivu(request.getNaziv()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Učionica već postoji");
        }
        Ucionica ucionica = new Ucionica();
        ucionica.setNaziv(request.getNaziv());
        return ucionice.save(ucionica).getId();
    }

    @PostMapping("/termini")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajTermin(@Valid @RequestBody TerminRequest request, Authentication auth) {
        if (!request.getVremePocetak().isBefore(request.getVremeKraja())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Početak mora biti pre kraja termina");
        }
        Predmet predmet = predmeti.findById(request.getPredmetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Predmet ne postoji"));
        proveriDaPredaje(auth, predmet.getId());
        Ucionica ucionica = ucionice.findById(request.getUcionicaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Učionica ne postoji"));
        Rok rok = rokovi.findById(request.getRokId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rok ne postoji"));
        if (termini.postojiPreklapanje(ucionica.getId(), request.getDatum(),
                request.getVremePocetak(), request.getVremeKraja())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Učionica je zauzeta u tom terminu");
        }
        Termin termin = new Termin();
        termin.setDatum(request.getDatum());
        termin.setVremePocetak(request.getVremePocetak());
        termin.setVremeKraj(request.getVremeKraja());
        termin.setUcionica(ucionica);
        termin.setPredmet(predmet);
        termin.setRok(rok);
        return termini.save(termin).getId();
    }

    @PostMapping("/grupe")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajGrupu(@Valid @RequestBody GrupaRequest request, Authentication auth) {
        Termin termin = termini.findById(request.getTerminId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Termin ne postoji"));
        if (termin.getPredmet() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Termin nema predmet");
        }
        proveriDaPredaje(auth, termin.getPredmet().getId());
        if (grupe.existsByNaziv(request.getNaziv())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Grupa već postoji");
        }
        Grupa grupa = new Grupa();
        grupa.setNaziv(request.getNaziv());
        grupa.setTermin(termin);
        return grupe.save(grupa).getId();
    }

    private static void proveriProfesora(Authentication auth) {
        boolean profesor = auth.getAuthorities().stream()
                .anyMatch(uloga -> "ROLE_PROFESOR".equals(uloga.getAuthority()));
        if (!profesor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Samo profesor može da menja raspored");
        }
    }

    private void proveriDaPredaje(Authentication auth, Long predmetId) {
        proveriProfesora(auth);
        boolean predaje = predajePredmet.getPredmetiZaPredavaca(auth.getName()).stream()
                .anyMatch(veza -> veza.getPredmet() != null && predmetId.equals(veza.getPredmet().getId()));
        if (!predaje) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profesor ne predaje ovaj predmet");
        }
    }
}
