package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.RokRequest;
import com.inzinjeri.rs_projekat.controllers.response.RokResponse;
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
@RequestMapping({"/api/rokovi", "/api/raspored/rokovi"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class RokController {
    private final RasporedService rasporedService;

    @GetMapping
    public List<RokResponse> svi() {
        return rasporedService.rokovi().stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RokResponse jedan(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.rok(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody RokRequest request, Authentication auth) {
        return rasporedService.dodajRok(request, auth);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public RokResponse izmeni(@PathVariable Long id, @Valid @RequestBody RokRequest request, Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniRok(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiRok(id, auth);
    }
}
