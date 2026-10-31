package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.PredajePredmetRequest;
import com.inzinjeri.rs_projekat.controllers.response.PredajePredmetResponse;
import com.inzinjeri.rs_projekat.services.PredajePredmetService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predaje-predmet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESOR')")
public class PredajePredmetController {
    private final PredajePredmetService predajePredmetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PredajePredmetResponse dodaj(@Valid @RequestBody PredajePredmetRequest request) {
        return EntityMappers.toResponse(predajePredmetService.dodaj(request));
    }
}
