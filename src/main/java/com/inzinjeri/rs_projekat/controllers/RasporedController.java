package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.GrupaRequest;
import com.inzinjeri.rs_projekat.controllers.request.RokRequest;
import com.inzinjeri.rs_projekat.controllers.request.TerminRequest;
import com.inzinjeri.rs_projekat.controllers.request.UcionicaRequest;
import com.inzinjeri.rs_projekat.controllers.response.*;
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
@RequestMapping("/api/raspored")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class RasporedController {

    private final RasporedService rasporedService;

    @GetMapping("/rokovi/{id}")
    public RokResponse rok(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.rok(id));
    }

    @GetMapping("/ucionice/{id}")
    public UcionicaResponse ucionica(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.ucionica(id));
    }

    @GetMapping("/termini/{id}")
    public TerminResponse termin(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.termin(id));
    }

    @GetMapping("/grupe/{id}")
    public GrupaResponse grupa(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.grupa(id));
    }

    @PutMapping("/rokovi/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public RokResponse izmeniRok(@PathVariable Long id, @Valid @RequestBody RokRequest request,
                                 Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniRok(id, request, auth));
    }

    @PutMapping("/ucionice/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public UcionicaResponse izmeniUcionicu(@PathVariable Long id, @Valid @RequestBody UcionicaRequest request,
                                          Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniUcionicu(id, request, auth));
    }

    @PutMapping("/termini/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public TerminResponse izmeniTerminAlias(@PathVariable Long id, @Valid @RequestBody TerminRequest request,
                                            Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniTermin(id, request, auth));
    }

    @PutMapping("/grupe/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public GrupaResponse izmeniGrupuAlias(@PathVariable Long id, @Valid @RequestBody GrupaRequest request,
                                         Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniGrupu(id, request, auth));
    }

    @DeleteMapping("/rokovi/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisiRok(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiRok(id, auth);
    }

    @DeleteMapping("/ucionice/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisiUcionicu(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiUcionicu(id, auth);
    }

    @DeleteMapping("/termini/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisiTermin(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiTermin(id, auth);
    }

    @DeleteMapping("/grupe/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisiGrupu(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiGrupu(id, auth);
    }

    @PutMapping("/termini/{id}")
    @PreAuthorize("@pristupService.profesorPredajePredmetTermina(authentication, #id)")
    public TerminResponse izmeniTermin(@PathVariable Long id, @Valid @RequestBody TerminRequest request,
                                       Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniTermin(id, request, auth));
    }

    @PutMapping("/grupe/{id}")
    @PreAuthorize("@pristupService.profesorPredajePredmetGrupe(authentication, #id)")
    public GrupaResponse izmeniGrupu(@PathVariable Long id, @Valid @RequestBody GrupaRequest request,
                                    Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniGrupu(id, request, auth));
    }

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
