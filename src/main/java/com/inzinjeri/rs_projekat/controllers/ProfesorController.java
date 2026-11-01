package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.ProfesorRequest;
import com.inzinjeri.rs_projekat.controllers.response.ProfesorResponse;
import com.inzinjeri.rs_projekat.services.NalogService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profesori")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class ProfesorController {
    private final NalogService nalogService;

    @PostMapping
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesorResponse dodaj(@Valid @RequestBody ProfesorRequest request) {
        return EntityMappers.toResponse(nalogService.dodajProfesora(request));
    }

    @GetMapping
    public List<ProfesorResponse> svi() {
        return nalogService.sviProfesori().stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProfesorResponse poId(@PathVariable Long id) {
        return EntityMappers.toResponse(nalogService.profesor(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESOR')")
    public ProfesorResponse moj(Authentication auth) {
        return EntityMappers.toResponse(nalogService.mojProfesor(auth));
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ProfesorResponse izmeni(@PathVariable Long id, @Valid @RequestBody ProfesorRequest request) {
        return EntityMappers.toResponse(nalogService.izmeniProfesora(id, request));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        nalogService.obrisiProfesora(id);
    }
}
