package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.PredajePredmetRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PredajePredmetService {
    private final PredajePredmetRepository predajePredmetRepository;
    private final ProfesorRepository profesorRepository;
    private final PredmetRepository predmetRepository;

    @Transactional
    public PredajePredmet dodaj(PredajePredmetRequest request) {
        Profesor profesor = profesorRepository.findByEmailIgnoreCase(request.getEmailProfesora().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor ne postoji"));
        Predmet predmet = predmetRepository.getPredmetByNaziv(request.getNazivPredmeta().trim())
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Predmet ne postoji"));

        if (predajePredmetRepository.existsByProfesor_IdAndPredmet_Id(profesor.getId(), predmet.getId())) {
            throw new ConflictException("Profesor vec predaje ovaj predmet");
        }
        PredajePredmet veza = new PredajePredmet();
        veza.setProfesor(profesor);
        veza.setPredmet(predmet);
        try {
            return predajePredmetRepository.saveAndFlush(veza);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Dodela predmeta nije moguca", e);
        }
    }
}
