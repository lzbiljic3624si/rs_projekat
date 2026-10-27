package com.inzinjeri.rs_projekat.controllers;

import com.inzinjeri.rs_projekat.controllers.request.PredmetRequest;
import com.inzinjeri.rs_projekat.controllers.response.PredmetResponse;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequiredArgsConstructor // lombok pravi konstruktor za predmetRepository, pa ga Spring prosleđuje kontroleru
public class PredmetController {

    private final PredmetRepository predmetRepository;

    @PostMapping("/add") // dodavanje predmeta
    @ResponseStatus(HttpStatus.CREATED)
    public Long add(@Valid @RequestBody PredmetRequest request) {
        if (!predmetRepository.getPredmetByNaziv(request.getNaziv()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Predmet sa tim nazivom već postoji");
        }

        Predmet predmet = new Predmet();
        predmet.setNaziv(request.getNaziv());
        predmet.setJezik(request.getJezik());
        predmet.setUslov(request.getUslov());
        return predmetRepository.save(predmet).getId();
    }

    @GetMapping("/all") // lista predmeta
    public List<PredmetResponse> all() {
        return predmetRepository.findAll().stream().map(PredmetController::toResponse).toList();
    }

    @GetMapping("/{id}") // jedan predmet
    public PredmetResponse byId(@PathVariable Long id) {
        return predmetRepository.findById(id)
            .map(PredmetController::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Predmet ne postoji"));
    }

    private static PredmetResponse toResponse(Predmet predmet) { // Predmet je objekat koji se čuva u bazi. PredmetResponse je ono što želiš da pošalješ klijentu. toResponse prepisuje id, naziv, jezik i uslov iz jednog u drugi
        PredmetResponse response = new PredmetResponse();
        response.setId(predmet.getId());
        response.setNaziv(predmet.getNaziv());
        response.setJezik(predmet.getJezik());
        response.setUslov(predmet.getUslov());
        return response;
    }
}
