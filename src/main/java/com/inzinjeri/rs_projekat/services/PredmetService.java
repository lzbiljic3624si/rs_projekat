package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PredmetService {
    private final PredmetRepository predmetRepository;

    public Predmet save(Predmet predmet) {
        String uslov = predmet.getUslov();
        if (uslov != null && (uslov.isBlank() || "0".equals(uslov.trim()))) {
            predmet.setUslov(null);
        }
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
