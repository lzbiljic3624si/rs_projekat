package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.TerminRequest;
import com.inzinjeri.rs_projekat.controllers.response.TerminResponse;
import com.inzinjeri.rs_projekat.services.RasporedService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/termini")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class TerminController {
    private final RasporedService rasporedService;

    @GetMapping
    public List<TerminResponse> svi(@RequestParam Long predmetId, @RequestParam Long rokId) {
        return rasporedService.termini(predmetId, rokId).stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TerminResponse jedan(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.termin(id));
    }

    @PostMapping
    @PreAuthorize("@pristupService.profesorPredajePredmet(authentication, #request.predmetId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody TerminRequest request, Authentication auth) {
        return rasporedService.dodajTermin(request, auth);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public TerminResponse izmeni(@PathVariable Long id, @Valid @RequestBody TerminRequest request, Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniTermin(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiTermin(id, auth);
    }
}
