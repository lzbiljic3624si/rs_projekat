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
@RequestMapping("/api/grupe")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class GrupaController {
    private final RasporedService rasporedService;

    @GetMapping
    public List<GrupaResponse> svi(@RequestParam Long terminId) {
        return rasporedService.grupe(terminId).stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/{id}")
    public GrupaResponse jedan(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.grupa(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody GrupaRequest request, Authentication auth) {
        return rasporedService.dodajGrupu(request, auth);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public GrupaResponse izmeni(@PathVariable Long id, @Valid @RequestBody GrupaRequest request, Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniGrupu(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiGrupu(id, auth);
    }
}
