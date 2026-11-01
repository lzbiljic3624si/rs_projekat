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
import com.inzinjeri.rs_projekat.services.ZadatakService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/zadaci")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class ZadatakController {

    private final ZadatakService zadatakService;

    @PutMapping("/{id}")
    @PreAuthorize("@pristupService.profesorPredajePredmetZadatka(authentication, #id)")
    public ZadatakResponse izmeni(@PathVariable Long id, @Valid @RequestBody ZadatakRequest request,
                                  Authentication auth) {
        return EntityMappers.toResponse(zadatakService.izmeni(id, request, auth));
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ZadatakResponse izmeniAlias(@PathVariable Long id, @Valid @RequestBody ZadatakRequest request,
                                      Authentication auth) {
        return EntityMappers.toResponse(zadatakService.izmeni(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        zadatakService.obrisi(id, auth);
    }

    @GetMapping("/grupa/{grupaId}")
    @PreAuthorize("@pristupService.studentPripadaGrupi(authentication, #grupaId) or "
            + "@pristupService.profesorPredajePredmetGrupe(authentication, #grupaId)")
    public ZadatakResponse zaGrupu(@PathVariable Long grupaId, Authentication auth) {
        return zadatakService.zaGrupu(grupaId, auth);
    }

    @PostMapping
    @PreAuthorize("@pristupService.profesorPredajePredmetGrupe(authentication, #request.grupaId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody ZadatakRequest request, Authentication auth) {
        return zadatakService.dodaj(request, auth);
    }
}
