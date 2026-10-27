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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rad")
@RequiredArgsConstructor
public class RadController {

    private final RadRepository radRepository;
    private final StudentRepository studentRepository;
    private final ZadatakRepository zadatakRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;

    @GetMapping("/moj")
    public List<RadResponse> moj(Authentication auth) {
        List<Rad> radovi = radRepository.getRadByStudentEmail(auth.getName());

        return radovi.stream().map(RadController::toResponse).toList();
    }

    @GetMapping("/predmet/{predmetId}/predati")
    public List<RadResponse> predati(@PathVariable Long predmetId, Authentication auth) {
        proveriDaPredaje(auth, predmetId);
        return radRepository.findByPredmetIdAndStatus(predmetId, "PREDAT")
                .stream().map(RadController::toResponse).toList();
    }

    @PostMapping("/{radId}/oceni")
    public RadResponse oceni(@PathVariable Long radId,
                             @Valid @RequestBody OcenaRadaRequest request,
                             Authentication auth) {
        Rad rad = radRepository.findById(radId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rad ne postoji"));

        if (rad.getZadatak() == null || rad.getZadatak().getGrupa() == null ||
                rad.getZadatak().getGrupa().getTermin() == null ||
                rad.getZadatak().getGrupa().getTermin().getPredmet() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rad nema predmet");
        }
        proveriDaPredaje(auth, rad.getZadatak().getGrupa().getTermin().getPredmet().getId());

        if (!"PREDAT".equals(rad.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rad nije spreman za ocenjivanje");
        }
        if (request.getPoeni() == null || !Double.isFinite(request.getPoeni()) || request.getPoeni() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poeni moraju biti nenegativan broj");
        }

        rad.setPoeni(request.getPoeni());
        rad.setStatus("OCENJEN");
        return toResponse(radRepository.save(rad));
    }

    @PostMapping("/{radId}/predaj")
    public RadResponse predaj(@PathVariable Long radId,
                              @Valid @RequestBody PredajaRadaRequest request,
                              Authentication auth) {
        Rad rad = radRepository.findById(radId)
                .orElseThrow(() -> new ResponseStatusException(
                   HttpStatus.NOT_FOUND, "Rad ne postoji"));

        if (rad.getStudent() == null ||
                !auth.getName().equals(rad.getStudent().getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Ovaj rad nije tvoj");
        }

        if (!"PREUZET".equals(rad.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Rad ne može ponovo da se preda");
        }

        rad.setRepoPutanjaResenja(request.getRepoPutanjaResenja());
        rad.setVremePredaje(LocalDateTime.now());
        rad.setStatus("PREDAT");

        return toResponse(radRepository.save(rad));
    }

    @PostMapping("/{zadatakId}/preuzmi")
    @ResponseStatus(HttpStatus.CREATED)
    public Long preuzmi(@PathVariable Long zadatakId, Authentication auth) {
        Student student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Prijavljeni korisnik nije student"));

        Zadatak zadatak = zadatakRepository.findById(zadatakId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Zadatak ne postoji"));

        if (zadatak.getGrupa() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Zadatak nije dodeljen grupi");
        }

        boolean smeDaPreuzme = prijavljenStudentRepository
                .existsByStudent_IdAndGrupa_IdAndStatus(
                        student.getId(),
                        zadatak.getGrupa().getId(),
                        "PRIHVACEN"
                );

        if (!smeDaPreuzme) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Student nije prihvaćen u grupu zadatka");
        }

        if (radRepository.existsByStudent_IdAndZadatak_Id(student.getId(), zadatakId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Zadatak je već preuzet");
        }

        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setVremePreuzimanja(LocalDateTime.now());
        rad.setStatus("PREUZET");

        return radRepository.save(rad).getId();
    }

    private void proveriDaPredaje(Authentication auth, Long predmetId) {
        boolean predaje = predajePredmetRepository.getPredmetiZaPredavaca(auth.getName())
                .stream().anyMatch(veza -> veza.getPredmet() != null &&
                        predmetId.equals(veza.getPredmet().getId()));
        if (!predaje) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profesor ne predaje ovaj predmet");
        }
    }

    private static RadResponse toResponse(Rad rad) {
        RadResponse response = new RadResponse();
        response.setId(rad.getId());
        response.setRepoPutanjaResenja(rad.getRepoPutanjaResenja());
        response.setPoeni(rad.getPoeni());
        response.setStatus(rad.getStatus());
        response.setVremePreuzimanja(rad.getVremePreuzimanja());
        response.setVremePredaje(rad.getVremePredaje());

        if(rad.getStudent() != null) {
            StudentResponse student = new StudentResponse();

            student.setId(rad.getStudent().getId());
            student.setIme(rad.getStudent().getIme());
            student.setPrezime(rad.getStudent().getPrezime());
            student.setBrIndeksa(rad.getStudent().getBrIndeksa());
            student.setGodUpisa(rad.getStudent().getGodUpisa());
            student.setEmail(rad.getStudent().getEmail());
            response.setStudent(student);
        }

        if(rad.getZadatak() != null) {
            ZadatakResponse zadatak = new ZadatakResponse();

            zadatak.setId(rad.getZadatak().getId());
            zadatak.setNaziv(rad.getZadatak().getNaziv());
            zadatak.setRepoPutanja(rad.getZadatak().getRepoPutanja());
            response.setZadatak(zadatak);
        }

        return response;
    }

}
