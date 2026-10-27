package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.response.PrijavljenStudentResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.PrijavljenStudent;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/prijave")
@RequiredArgsConstructor
public class PrijavaController {

    private final PrijavljenStudentRepository prijave;
    private final StudentRepository studenti;
    private final GrupaRepository grupe;
    private final PredajePredmetRepository predajePredmet;

    @PostMapping("/grupa/{grupaId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Long prijaviSe(@PathVariable Long grupaId, Authentication auth) {
        var student = studenti.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Prijavljeni korisnik nije student"));
        Grupa grupa = grupe.findById(grupaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupa ne postoji"));
        predmetId(grupa);
        if (prijave.existsByStudent_IdAndGrupa_Id(student.getId(), grupaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student je već prijavljen u grupu");
        }
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setStudent(student);
        prijava.setGrupa(grupa);
        prijava.setStatus("NA_CEKANJU");
        return prijave.save(prijava).getId();
    }

    @GetMapping("/moje")
    public List<PrijavljenStudentResponse> moje(Authentication auth) {
        studenti.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Prijavljeni korisnik nije student"));
        return prijave.getStudentByEmail(auth.getName()).stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/grupa/{grupaId}")
    public List<PrijavljenStudentResponse> prijaveZaGrupu(@PathVariable Long grupaId, Authentication auth) {
        Grupa grupa = grupe.findById(grupaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupa ne postoji"));
        proveriProfesora(auth, grupa);
        return prijave.findByGrupa_Id(grupaId).stream().map(EntityMappers::toResponse).toList();
    }

    @PostMapping("/{prijavaId}/prihvati")
    public PrijavljenStudentResponse prihvati(@PathVariable Long prijavaId, Authentication auth) {
        PrijavljenStudent prijava = prijave.findById(prijavaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prijava ne postoji"));
        proveriProfesora(auth, prijava.getGrupa());
        if (!"NA_CEKANJU".equals(prijava.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prijava više nije na čekanju");
        }
        prijava.setStatus("PRIHVACEN");
        return EntityMappers.toResponse(prijave.save(prijava));
    }

    private void proveriProfesora(Authentication auth, Grupa grupa) {
        if (auth.getAuthorities().stream().noneMatch(uloga -> "ROLE_PROFESOR".equals(uloga.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Samo profesor moze da prihvati prijavu");
        }
        Long predmetId = predmetId(grupa);
        boolean predaje = predajePredmet.getPredmetiZaPredavaca(auth.getName()).stream()
                .anyMatch(veza -> veza.getPredmet() != null && predmetId.equals(veza.getPredmet().getId()));
        if (!predaje) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profesor ne predaje predmet grupe");
        }
    }

    private static Long predmetId(Grupa grupa) {
        if (grupa == null || grupa.getTermin() == null || grupa.getTermin().getPredmet() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Grupa nema predmet");
        }
        return grupa.getTermin().getPredmet().getId();
    }
}
