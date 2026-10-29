package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.repositories.PredmetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PredmetService {
    private final PredmetRepository predmetRepository;

    public Predmet save(Predmet predmet) {
        if (!predmetRepository.getPredmetByNaziv(predmet.getNaziv()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Predmet sa tim nazivom vec postoji"
            );
        }

        return predmetRepository.save(predmet);
    }

    public List<Predmet> findAll() {
        return predmetRepository.findAll();
    }

    public Predmet findById(Long id) {
        return predmetRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Predmet ne postoji"
        ));
    }
}
