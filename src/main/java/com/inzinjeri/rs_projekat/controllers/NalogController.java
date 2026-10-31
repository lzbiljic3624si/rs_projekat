package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.ProfesorRequest;
import com.inzinjeri.rs_projekat.controllers.request.StudentRequest;
import com.inzinjeri.rs_projekat.controllers.response.ProfesorResponse;
import com.inzinjeri.rs_projekat.controllers.response.StudentResponse;
import com.inzinjeri.rs_projekat.services.NalogService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nalozi")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESOR')")
public class NalogController {
    private final NalogService nalogService;

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
