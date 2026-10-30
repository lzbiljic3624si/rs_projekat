package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.response.PrijavljenStudentResponse;
import com.inzinjeri.rs_projekat.services.PrijavaService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prijave")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
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
    @PreAuthorize("@pristupService.profesorPredajePredmetGrupe(authentication, #grupaId)")
    public List<PrijavljenStudentResponse> prijaveZaGrupu(@PathVariable Long grupaId, Authentication auth) {
        return prijavaService.prijavaZaGrupu(grupaId, auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

}
