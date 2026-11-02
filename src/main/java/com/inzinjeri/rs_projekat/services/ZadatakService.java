package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.ZadatakRequest;
import com.inzinjeri.rs_projekat.controllers.response.ZadatakResponse;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.EntityMappers;
import com.inzinjeri.rs_projekat.utils.VremeTermina;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ZadatakService {

    private final ZadatakRepository zadatakRepository;
    private final GrupaRepository grupaRepository;
    private final StudentRepository studentRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final RadRepository radRepository;
    private final Clock clock;

    @Transactional
    public Zadatak izmeni(Long id, ZadatakRequest request, Authentication auth) {
        Zadatak zadatak = zaProfesora(id, auth);
        Grupa grupa = zadatak.getGrupa();
        if (!grupa.getId().equals(request.getGrupaId())) {
            throw new ConflictException("Zadatak ne moze da promeni grupu");
        }
        if (radRepository.existsByZadatak_Id(id)) {
            throw new ConflictException("Zadatak je vec preuzet i ne moze da se menja");
        }
        if (!request.getRepoPutanja().equals(zadatak.getRepoPutanja())
                && zadatakRepository.existsByRepoPutanja(request.getRepoPutanja())) {
            throw new ConflictException("Putanja zadatka vec postoji");
        }
        zadatak.setNaziv(request.getNaziv());
        zadatak.setRepoPutanja(request.getRepoPutanja());
        return zadatakRepository.save(zadatak);
    }

    @Transactional
    public void obrisi(Long id, Authentication auth) {
        Zadatak zadatak = zaProfesora(id, auth);
        if (radRepository.existsByZadatak_Id(id)) {
            throw new ConflictException("Zadatak ima radove");
        }
        zadatakRepository.delete(zadatak);
        zadatakRepository.flush();
    }

    private Zadatak zaProfesora(Long id, Authentication auth) {
        Zadatak zadatak = zadatakRepository.findByIdZaIzmenu(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji"));
        Grupa grupa = zadatak.getGrupa();
        if (grupa == null) {
            throw new ConflictException("Zadatak nema grupu");
        }
        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ForbiddenException("Profesor ne predaje predmet grupe");
        }
        return zadatak;
    }

    @Transactional
    public ZadatakResponse zaGrupu(Long grupaId, Authentication auth) {
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));

        Long predmetId = predmetId(grupa);

        boolean studentGrupe = studentRepository.pronadjiPoEmailu(auth.getName())
                .map(student -> prijavljenStudentRepository
                        .existsByStudent_IdAndGrupa_Id(student.getId(), grupaId))
                .orElse(false);

        boolean profesorPredmeta = profesorPredajePredmet(auth, predmetId);
        if (!studentGrupe && !profesorPredmeta) {
            throw new ForbiddenException("Nemaš pristup zadatku ove grupe");
        }

        if (!profesorPredmeta) {
            VremeTermina.proveriPreuzimanje(grupa.getTermin(), LocalDateTime.now(clock));
        }

        Zadatak zadatak = zadatakRepository.pronadjiPoGrupi(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa nema zadatak"));

        return EntityMappers.toResponse(zadatak);
    }

    public Long dodaj(ZadatakRequest request, Authentication auth) {
        Grupa grupa = grupaRepository.findById(request.getGrupaId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));

        if (!profesorPredajePredmet(auth, predmetId(grupa))) {
            throw new ForbiddenException("Profesor ne predaje predmet grupe");
        }

        if (zadatakRepository.existsByGrupa_Id(grupa.getId())) {
            throw new ConflictException("Grupa već ima zadatak");
        }

        if (zadatakRepository.existsByRepoPutanja(request.getRepoPutanja())) {
            throw new ConflictException("Putanja zadatka već postoji");
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
            throw new ConflictException("Grupa nema predmet");
        }

        return grupa.getTermin().getPredmet().getId();
    }
}
