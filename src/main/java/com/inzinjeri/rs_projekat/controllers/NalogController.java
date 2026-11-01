package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.ProfesorRequest;
import com.inzinjeri.rs_projekat.controllers.request.StudentRequest;
import com.inzinjeri.rs_projekat.controllers.request.NalogRequest;
import com.inzinjeri.rs_projekat.controllers.response.NalogResponse;
import com.inzinjeri.rs_projekat.controllers.response.ProfesorResponse;
import com.inzinjeri.rs_projekat.controllers.response.StudentResponse;
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
@RequestMapping("/api/nalozi")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESOR')")
public class NalogController {
    private final NalogService nalogService;

    @GetMapping
    public List<NalogResponse> svi() {
        return nalogService.sviNalozi().stream().map(NalogResponse::from).toList();
    }

    @GetMapping("/{id}")
    public NalogResponse poId(@PathVariable Long id) {
        return NalogResponse.from(nalogService.nalog(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
    public NalogResponse moj(Authentication auth) {
        return NalogResponse.from(nalogService.mojNalog(auth));
    }

    @PutMapping("/edit/{id}")
    public NalogResponse izmeni(@PathVariable Long id, @Valid @RequestBody NalogRequest request) {
        return NalogResponse.from(nalogService.izmeniNalog(id, request));
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        nalogService.obrisiNalog(id);
    }

    @PostMapping("/studenti")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse dodajStudenta(@Valid @RequestBody StudentRequest request) {
        return EntityMappers.toResponse(nalogService.dodajStudenta(request));
    }

    @PostMapping("/profesori")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesorResponse dodajProfesora(@Valid @RequestBody ProfesorRequest request) {
        return EntityMappers.toResponse(nalogService.dodajProfesora(request));
    }
}
