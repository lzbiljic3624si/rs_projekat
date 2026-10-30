package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.GrupaRequest;
import com.inzinjeri.rs_projekat.controllers.request.RokRequest;
import com.inzinjeri.rs_projekat.controllers.request.TerminRequest;
import com.inzinjeri.rs_projekat.controllers.request.UcionicaRequest;
import com.inzinjeri.rs_projekat.controllers.response.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.RasporedService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/raspored")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class RasporedController {

    private final RasporedService rasporedService;

    @GetMapping("/rokovi")
    public List<RokResponse> rokovi() {
        return rasporedService.rokovi()
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/ucionice")
    public List<UcionicaResponse> ucionice() {
        return rasporedService.ucionice()
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/termini")
    public List<TerminResponse> termini(@RequestParam Long predmetId, @RequestParam Long rokId) {
        return rasporedService.termini(predmetId, rokId)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/grupe")
    public List<GrupaResponse> grupe(@RequestParam Long terminId) {
        return rasporedService.grupe(terminId)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @PostMapping("/rokovi")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajRok(@Valid @RequestBody RokRequest request, Authentication auth) {
        return rasporedService.dodajRok(request, auth);
    }

    @PostMapping("/ucionice")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajUcionicu(@Valid @RequestBody UcionicaRequest request, Authentication auth) {
        return rasporedService.dodajUcionicu(request, auth);
    }

    @PostMapping("/termini")
    @PreAuthorize("@pristupService.profesorPredajePredmet(authentication, #request.predmetId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajTermin(@Valid @RequestBody TerminRequest request, Authentication auth) {
        return rasporedService.dodajTermin(request, auth);
    }

    @PostMapping("/grupe")
    @PreAuthorize("@pristupService.profesorPredajePredmetTermina(authentication, #request.terminId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodajGrupu(@Valid @RequestBody GrupaRequest request, Authentication auth) {
        return rasporedService.dodajGrupu(request, auth);
    }
}
