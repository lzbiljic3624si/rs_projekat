package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.UcionicaRequest;
import com.inzinjeri.rs_projekat.controllers.response.UcionicaResponse;
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
@RequestMapping("/api/ucionice")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class UcionicaController {
    private final RasporedService rasporedService;

    @GetMapping
    public List<UcionicaResponse> svi() {
        return rasporedService.ucionice().stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UcionicaResponse jedan(@PathVariable Long id) {
        return EntityMappers.toResponse(rasporedService.ucionica(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody UcionicaRequest request, Authentication auth) {
        return rasporedService.dodajUcionicu(request, auth);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public UcionicaResponse izmeni(@PathVariable Long id, @Valid @RequestBody UcionicaRequest request, Authentication auth) {
        return EntityMappers.toResponse(rasporedService.izmeniUcionicu(id, request, auth));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id, Authentication auth) {
        rasporedService.obrisiUcionicu(id, auth);
    }
}
