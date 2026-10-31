package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.OcenaRadaRequest;
import com.inzinjeri.rs_projekat.controllers.request.PredajaRadaRequest;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.StatusRada;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RadService {

    private final RadRepository radRepository;
    private final StudentRepository studentRepository;
    private final ZadatakRepository zadatakRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;

    @Transactional(readOnly = true)
    public List<Rad> moj(Authentication auth) {
        return radRepository.getRadByStudentEmail(auth.getName());
    }

    @Transactional(readOnly = true)
    public List<Rad> predati(Long predmetId, Authentication auth) {
        proveriDaPredaje(auth, predmetId);

        return radRepository.findByPredmetIdAndStatus(predmetId, StatusRada.PREDAT);
    }

    @Transactional
    public Rad oceni (Long radId, OcenaRadaRequest request, Authentication auth){
        Rad rad = radRepository.findById(radId)
                .orElseThrow(() ->  new ResourceNotFoundException("Rad ne postoji"));

        if(rad.getZadatak() == null
                || rad.getZadatak().getGrupa() == null
                || rad.getZadatak().getGrupa().getTermin() == null
                || rad.getZadatak().getGrupa().getTermin().getPredmet() == null){
            throw new ConflictException("Rad nema predmet");
        }

        Long predmetId = rad.getZadatak()
                .getGrupa()
                .getTermin()
                .getPredmet()
                .getId();

        proveriDaPredaje(auth, predmetId);

        if(rad.getStatus() != StatusRada.PREDAT){
            throw  new ConflictException("Rad nije spreman za ocenjivanje");
        }

        if(request.getPoeni() == null
                || !Double.isFinite(request.getPoeni())
                || request.getPoeni() < 0){
            throw new BadRequestException("Poeni moraju biti nenegativan broj");
        }

        rad.setPoeni(request.getPoeni());
        rad.setStatus(StatusRada.OCENJEN);

        return radRepository.save(rad);
    }

    @Transactional
    public Rad predaj(Long radId, PredajaRadaRequest request, Authentication auth){
        Rad rad = radRepository.findById(radId)
                .orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
        if(rad.getStudent() == null
                || !auth.getName().equals(rad.getStudent().getEmail())){
            throw new ForbiddenException("Ovaj rad nije tvoj");
        }
        if(rad.getStatus() != StatusRada.PREUZET){
            throw new ConflictException("Rad ne moze ponovo da se preda");
        }

        rad.setRepoPutanjaResenja(request.getRepoPutanjaResenja());
        rad.setVremePredaje(LocalDateTime.now());
        rad.setStatus(StatusRada.PREDAT);

        return radRepository.save(rad);
    }

    @Transactional
    public Long preuzmi(Long zadatakId, Authentication auth){
        Student student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));

        Zadatak zadatak = zadatakRepository.findById(zadatakId)
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji"));

        if (zadatak.getGrupa() == null) {
            throw new ConflictException("Zadatku nije dodeljena grupa");
        }

        boolean smeDaPreuzme = prijavljenStudentRepository
                .existsByStudent_IdAndGrupa_Id(
                        student.getId(),
                        zadatak.getGrupa().getId()
                );

        if (!smeDaPreuzme) {
            throw new ForbiddenException("Grupa zadatka nije studentova");
        }

        if(radRepository.existsByStudent_IdAndZadatak_Id(student.getId(),zadatakId)){
            throw new ConflictException("Zadatak je vec preuzet");
        }

        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setVremePreuzimanja(LocalDateTime.now());
        rad.setStatus(StatusRada.PREUZET);

        return radRepository.save(rad).getId();
    }

    private void proveriDaPredaje(Authentication auth, Long predmetId) {
        boolean predaje = predajePredmetRepository.getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza -> veza.getPredmet() != null
                        && predmetId.equals(veza.getPredmet().getId()));

        if (!predaje) {
            throw new ForbiddenException("Profesor ne predaje ovaj predmet");
        }
    }
}
