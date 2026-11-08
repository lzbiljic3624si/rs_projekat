package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.TerminRepository;
import com.inzinjeri.rs_projekat.controllers.request.PredmetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PredmetService {
    private final PredmetRepository predmetRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final TerminRepository terminRepository;

    @Transactional
    public Predmet izmeni(Long id, PredmetRequest request) {
        Predmet predmet = findById(id);
        if (predmetRepository.getPredmetByNaziv(request.getNaziv()).stream()
                .anyMatch(p -> !id.equals(p.getId()))) {
            throw new ConflictException("Predmet sa tim nazivom vec postoji");
        }
        predmet.setNaziv(request.getNaziv());
        predmet.setJezik(request.getJezik());
        return predmetRepository.save(predmet);
    }

    @Transactional
    public void obrisi(Long id) {
        Predmet predmet = findById(id);
        if (predajePredmetRepository.existsByPredmet_Id(id) || terminRepository.existsByPredmet_Id(id)) {
            throw new ConflictException("Predmet ima dodele profesora ili termine");
        }
        predmetRepository.delete(predmet);
        predmetRepository.flush();
    }

    public Predmet save(Predmet predmet) {
        if (!predmetRepository.getPredmetByNaziv(predmet.getNaziv()).isEmpty()) {
            throw new ConflictException("Predmet sa tim nazivom vec postoji");
        }

        return predmetRepository.save(predmet);
    }

    public List<Predmet> findAll() {
        return predmetRepository.findAll();
    }

    public Predmet findById(Long id) {
        return predmetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Predmet ne postoji"));
    }
}
