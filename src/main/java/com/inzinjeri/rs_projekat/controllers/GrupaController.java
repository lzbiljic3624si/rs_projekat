package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.GrupaRequest;
import com.inzinjeri.rs_projekat.controllers.response.GrupaResponse;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class GrupaController {
    private final RasporedService rasporedService;

    @GetMapping({"/grupe", "/raspored/grupe"})
    public List<GrupaResponse> svi(@RequestParam Long terminId) {
        return rasporedService.grupe(terminId).stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping({"/grupe/{id}", "/raspored/grupe/{id}"})
    public GrupaResponse jedan(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.grupa(id));
    }

    @PostMapping({"/grupe", "/raspored/grupe"})
    @PreAuthorize("@pristupService.profesorPredajePredmetTermina(authentication, #request.terminId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody GrupaRequest request, Authentication auth) {
        return rasporedService.dodajGrupu(request, auth);
    }

    @PutMapping({"/grupe/edit/{id}", "/raspored/grupe/edit/{id}"})
    @PreAuthorize("hasRole('PROFESOR')")
    public GrupaResponse izmeni(@PathVariable Long id, @Valid @RequestBody GrupaRequest request, Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniGrupu(id, request, auth));
    }

    @PutMapping("/raspored/grupe/{id}")
    @PreAuthorize("@pristupService.profesorPredajePredmetGrupe(authentication, #id)")
    public GrupaResponse izmeniStaromPutanjom(@PathVariable Long id, @Valid @RequestBody GrupaRequest request,
                                              Authentication auth) {
        return izmeni(id, request, auth);
    }

    @DeleteMapping({"/grupe/delete/{id}", "/raspored/grupe/delete/{id}"})
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiGrupu(id, auth);
    }
}
