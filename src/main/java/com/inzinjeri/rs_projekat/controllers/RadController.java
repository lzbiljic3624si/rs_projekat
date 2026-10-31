package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.PredajaRadaRequest;
import com.inzinjeri.rs_projekat.controllers.request.OcenaRadaRequest;
import com.inzinjeri.rs_projekat.controllers.response.RadResponse;
import com.inzinjeri.rs_projekat.controllers.response.StudentResponse;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.repositories.ZadatakRepository;
import com.inzinjeri.rs_projekat.services.RadService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rad")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class RadController {

    private final RadService radService;

    @GetMapping("/moj")
    public List<RadResponse> moj(Authentication auth) {
        return radService.moj(auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/predmet/{predmetId}/predati")
    @PreAuthorize("@pristupService.profesorPredajePredmet(authentication, #predmetId)")
    public List<RadResponse> predati(@PathVariable Long predmetId, Authentication auth) {
        return radService.predati(predmetId, auth)
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @PostMapping("/{radId}/oceni")
    @PreAuthorize("@pristupService.profesorPredajePredmetRada(authentication, #radId)")
    public RadResponse oceni(@PathVariable Long radId,
                             @Valid @RequestBody OcenaRadaRequest request,
                             Authentication auth) {
        return EntityMappers.toResponse(radService.oceni(radId, request, auth));
    }

    @PostMapping("/{radId}/predaj")
    @PreAuthorize("@pristupService.studentJeVlasnikRada(authentication, #radId)")
    public RadResponse predaj(@PathVariable Long radId,
                              @Valid @RequestBody PredajaRadaRequest request,
                              Authentication auth) {


        return EntityMappers.toResponse(radService.predaj(radId, request, auth));
    }

    @PostMapping("/{zadatakId}/preuzmi")
    @PreAuthorize("@pristupService.studentImaPristupZadatku(authentication, #zadatakId)")
    @ResponseStatus(HttpStatus.CREATED)
    public Long preuzmi(@PathVariable Long zadatakId, Authentication auth) {
        return radService.preuzmi(zadatakId, auth);
    }





}
