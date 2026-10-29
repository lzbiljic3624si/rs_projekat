package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import com.inzinjeri.rs_projekat.services.ZadatakService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/zadaci")
@RequiredArgsConstructor
public class ZadatakController {

    private final ZadatakService zadatakService;

    @GetMapping("/grupa/{grupaId}")
    public ZadatakResponse zaGrupu(@PathVariable Long grupaId, Authentication auth) {
        return zadatakService.zaGrupu(grupaId, auth);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long dodaj(@Valid @RequestBody ZadatakRequest request, Authentication auth) {
        return zadatakService.dodaj(request, auth);
    }
}
