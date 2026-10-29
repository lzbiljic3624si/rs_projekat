package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.response.PrijavljenStudentResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.PrijavljenStudent;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.services.PrijavaService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/prijave")
@RequiredArgsConstructor
public class PrijavaController {

    private final PrijavaService prijavaService;

    @PostMapping("/grupa/{grupaId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Long prijaviSe(@PathVariable Long grupaId, Authentication auth) {
        return  prijavaService.prijaviSe(grupaId, auth);
    }

    @GetMapping("/moje")
    public List<PrijavljenStudentResponse> moje(Authentication auth) {
        return prijavaService.moje(auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/grupa/{grupaId}")
    public List<PrijavljenStudentResponse> prijaveZaGrupu(@PathVariable Long grupaId, Authentication auth) {
        return prijavaService.prijavaZaGrupu(grupaId, auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @PostMapping("/{prijavaId}/prihvati")
    public PrijavljenStudentResponse prihvati(@PathVariable Long prijavaId, Authentication auth) {
        PrijavljenStudent prijava = prijavaService.prihvati(prijavaId, auth);

        return EntityMappers.toResponse(prijava);
    }




}
