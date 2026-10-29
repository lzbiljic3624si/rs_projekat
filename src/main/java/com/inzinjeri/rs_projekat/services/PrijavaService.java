package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.PrijavljenStudent;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrijavaService {
    private final PrijavljenStudentRepository prijavljanStudentRepository;
    private final StudentRepository studentRepository;
    private final GrupaRepository grupaRepository;
    private final PredajePredmetRepository predajePredmetRepository;

    @Transactional
    public Long prijaviSe(Long grupaId, Authentication auth) {
        var student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Prijavljeni korisnik nije student"
                ));
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grupa ne postoji"
                ));
        predmetId(grupa);

        if (prijavljanStudentRepository.existsByStudent_IdAndGrupa_Id(student.getId(), grupaId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student je vec prijavljen u grupu"
            );
        }

        PrijavljenStudent prijavljenStudent = new PrijavljenStudent();
        prijavljenStudent.setStudent(student);
        prijavljenStudent.setGrupa(grupa);
        prijavljenStudent.setStatus("NA_CEKANJU"); //mozda ne treba

        return prijavljanStudentRepository.save(prijavljenStudent).getId();
    }

    @Transactional(readOnly = true)
    public List<PrijavljenStudent> moje(Authentication auth) {
        studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Prijavljeni korisnik nije student"
                ));

        return prijavljanStudentRepository.getStudentByEmail(auth.getName());
    }

    @Transactional(readOnly = true)
    public List<PrijavljenStudent> prijavaZaGrupu(Long grupaId, Authentication auth) {
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Grupa ne postoji"
                ));
        proveriProfesora(auth, grupa);

        return prijavljanStudentRepository.findByGrupa_Id(grupaId);
    }

    @Transactional
    public PrijavljenStudent prihvati(Long prijavaId, Authentication auth) {
        PrijavljenStudent prijava = prijavljanStudentRepository.findById(prijavaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Prijava ne postoji"
                ));

        proveriProfesora(auth, prijava.getGrupa());

        if (!"NA_CEKANJU".equals(prijava.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Prijava vise nije na cekanju"
            );
        }

        prijava.setStatus("PRIHVACEN");

        return prijavljanStudentRepository.save(prijava);
    }


    private void proveriProfesora(Authentication auth, Grupa grupa) {
        if (auth.getAuthorities()
                .stream()
                .noneMatch(uloga -> "ROLE_PROFESOR"
                        .equals(uloga.getAuthority()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Samo profesor moze da prihvati prijavu"
            );
        }

        Long idPredmeta = predmetId(grupa);

        boolean predaje = predajePredmetRepository
                .getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza ->
                        veza.getPredmet() != null
                                && idPredmeta.equals(veza.getPredmet().getId())
                );

        if (!predaje) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Profesor ne predaje predmet grupe"
            );
        }
    }

    private static Long predmetId(Grupa grupa) {
        if (grupa == null
                || grupa.getTermin() == null
                || grupa.getTermin().getPredmet() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Grupa ne postoji"
            );
        }
        return grupa.getTermin().getPredmet().getId();
    }
}
