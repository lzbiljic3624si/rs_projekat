package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.PredmetRequest;
import com.inzinjeri.rs_projekat.controllers.response.PredmetResponse;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import com.inzinjeri.rs_projekat.services.PredmetService;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController // prima web zahteve i vraca podatke kao JSON
@RequestMapping("/api/predmet") // zajednicki pocetak adrese za sve metode u klasi
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESOR')")
public class PredmetController {

    private final PredmetService predmetService;

    @PostMapping("/add") // dodavanje predmeta
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESOR')")
    public Long add(@Valid @RequestBody PredmetRequest request) {

        Predmet predmet = new Predmet();
        predmet.setNaziv(request.getNaziv());
        predmet.setJezik(request.getJezik());
        predmet.setUslov(request.getUslov());

        return predmetService.save(predmet).getId();
    }

    @GetMapping("/all") // lista predmeta
    public List<PredmetResponse> all() {
        return predmetService.findAll()
                .stream()
                .map(EntityMappers::toResponse)
                .toList();
    }

    @GetMapping("/{id}") // jedan predmet
    public PredmetResponse byId(@PathVariable Long id) {
        return EntityMappers.toResponse(predmetService.findById(id));
    }
}
