package com.inzinjeri.rs_projekat.services;


import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ZadatakService {

    private final ZadatakRepository zadatakRepository;
    private final GrupaRepository grupaRepository;
    private final StudentRepository studentRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;

    @Transactional
    public ZadatakResponse zaGrupu(Long grupaId, Authentication auth) {
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Grupa ne postoji"));

        Long predmetId = predmetId(grupa);

        boolean studentGrupe = studentRepository.pronadjiPoEmailu(auth.getName())
                .map(student -> prijavljenStudentRepository
                        .existsByStudent_IdAndGrupa_Id(student.getId(), grupaId))
                .orElse(false);

        if (!studentGrupe && !profesorPredajePredmet(auth, predmetId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Nemaš pristup zadatku ove grupe");
        }

        Zadatak zadatak = zadatakRepository.pronadjiPoGrupi(grupaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Grupa nema zadatak"));

        return EntityMappers.toResponse(zadatak);
    }

    public Long dodaj(ZadatakRequest request, Authentication auth) {
        Grupa grupa = grupaRepository.findById(request.getGrupaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Grupa ne postoji"));

        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Profesor ne predaje predmet grupe");
        }

        if (zadatakRepository.existsByGrupa_Id(grupa.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Grupa već ima zadatak");
        }

        if (zadatakRepository.existsByRepoPutanja(request.getRepoPutanja())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Putanja zadatka već postoji");
        }

        Zadatak zadatak = new Zadatak();
        zadatak.setNaziv(request.getNaziv());
        zadatak.setGrupa(grupa);
        zadatak.setRepoPutanja(request.getRepoPutanja());

        return zadatakRepository.save(zadatak).getId();
    }

    private boolean profesorPredajePredmet(
            Authentication auth, Long predmetId) {

        if (auth.getAuthorities().stream()
                .noneMatch(uloga ->
                        "ROLE_PROFESOR".equals(uloga.getAuthority()))) {
            return false;
        }

        return predajePredmetRepository
                .getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza ->
                        veza.getPredmet() != null
                                && predmetId.equals(
                                veza.getPredmet().getId()));
    }

    private static Long predmetId(Grupa grupa) {
        if (grupa.getTermin() == null
                || grupa.getTermin().getPredmet() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Grupa nema predmet");
        }

        return grupa.getTermin().getPredmet().getId();
    }
}
