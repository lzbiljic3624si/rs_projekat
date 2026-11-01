package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.StudentRequest;
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
@RequestMapping("/api/studenti")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESOR')")
public class StudentController {
    private final NalogService nalogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse dodaj(@Valid @RequestBody StudentRequest request) {
        return EntityMappers.toResponse(nalogService.dodajStudenta(request));
    }

    @GetMapping
    public List<StudentResponse> svi() {
        return nalogService.sviStudenti().stream().map(EntityMappers::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
    public StudentResponse poId(@PathVariable Long id, Authentication auth) {
        return EntityMappers.toResponse(nalogService.student(id, auth));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentResponse moj(Authentication auth) {
        return EntityMappers.toResponse(nalogService.mojStudent(auth));
    }

    @PutMapping("/edit/{id}")
    public StudentResponse izmeni(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return EntityMappers.toResponse(nalogService.izmeniStudenta(id, request));
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        nalogService.obrisiStudenta(id);
    }
}
