package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/zadaci")
@RequiredArgsConstructor
public class ZadatakController {

    private final ZadatakRepository zadaci;
    private final GrupaRepository grupe;
    private final StudentRepository studenti;
    private final PrijavljenStudentRepository prijave;
    private final PredajePredmetRepository predajePredmet;

    @GetMapping("/grupa/{grupaId}")
    public List<ZadatakResponse> zaGrupu(@PathVariable Long grupaId, Authentication auth) {
        Grupa grupa = grupe.findById(grupaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupa ne postoji"));
        Long predmetId = predmetId(grupa);
        boolean studentGrupe = studenti.pronadjiPoEmailu(auth.getName())
                .map(student -> prijave.existsByStudent_IdAndGrupa_IdAndStatus(
                        student.getId(), grupaId, "PRIHVACEN"))
                .orElse(false);
        if (!studentGrupe && !profesorPredajePredmet(auth, predmetId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemaš pristup zadacima ove grupe");
        }
        return zadaci.pronadjiPoGrupi(grupaId).stream().map(EntityMappers::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody ZadatakRequest request, Authentication auth) {
        Grupa grupa = grupe.findById(request.getGrupaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupa ne postoji"));
        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profesor ne predaje predmet grupe");
        }
        if (zadaci.existsByRepoPutanja(request.getRepoPutanja())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Putanja zadatka već postoji");
        }
        Zadatak zadatak = new Zadatak();
        zadatak.setNaziv(request.getNaziv());
        zadatak.setGrupa(grupa);
        zadatak.setRepoPutanja(request.getRepoPutanja());
        return zadaci.save(zadatak).getId();
    }

    private boolean profesorPredajePredmet(Authentication auth, Long predmetId) {
        if (auth.getAuthorities().stream().noneMatch(uloga -> "ROLE_PROFESOR".equals(uloga.getAuthority()))) {
            return false;
        }
        return predajePredmet.getPredmetiZaPredavaca(auth.getName()).stream()
                .anyMatch(veza -> veza.getPredmet() != null && predmetId.equals(veza.getPredmet().getId()));
    }

    private static Long predmetId(Grupa grupa) {
        if (grupa.getTermin() == null || grupa.getTermin().getPredmet() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Grupa nema predmet");
        }
        return grupa.getTermin().getPredmet().getId();
    }
}
