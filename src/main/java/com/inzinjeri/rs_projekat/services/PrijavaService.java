package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.Grupa;
import com.inzinjeri.rs_projekat.model.PrijavljenStudent;
import com.inzinjeri.rs_projekat.repositories.GrupaRepository;
import com.inzinjeri.rs_projekat.repositories.PredajePredmetRepository;
import com.inzinjeri.rs_projekat.repositories.PrijavljenStudentRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        predmetId(grupa);

        if (prijavljanStudentRepository.existsByStudent_IdAndGrupa_Id(student.getId(), grupaId)) {
            throw new ConflictException("Student je vec prijavljen u grupu");
        }

        PrijavljenStudent prijavljenStudent = new PrijavljenStudent();
        prijavljenStudent.setStudent(student);
        prijavljenStudent.setGrupa(grupa);

        return prijavljanStudentRepository.save(prijavljenStudent).getId();
    }

    @Transactional(readOnly = true)
    public List<PrijavljenStudent> moje(Authentication auth) {
        studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));

        return prijavljanStudentRepository.getStudentByEmail(auth.getName());
    }

    @Transactional(readOnly = true)
    public List<PrijavljenStudent> prijavaZaGrupu(Long grupaId, Authentication auth) {
        Grupa grupa = grupaRepository.findById(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        proveriProfesora(auth, grupa);

        return prijavljanStudentRepository.findByGrupa_Id(grupaId);
    }

    private void proveriProfesora(Authentication auth, Grupa grupa) {
        if (auth.getAuthorities()
                .stream()
                .noneMatch(uloga -> "ROLE_PROFESOR"
                        .equals(uloga.getAuthority()))) {
            throw new ForbiddenException("Samo profesor moze da vidi prijave grupe");
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
            throw new ForbiddenException("Profesor ne predaje predmet grupe");
        }
    }

    private static Long predmetId(Grupa grupa) {
        if (grupa == null
                || grupa.getTermin() == null
                || grupa.getTermin().getPredmet() == null) {
            throw new ConflictException("Grupa ne postoji");
        }
        return grupa.getTermin().getPredmet().getId();
    }
}
