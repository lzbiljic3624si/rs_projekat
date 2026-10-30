package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("pristupService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PristupService {
    private final PredajePredmetRepository predajePredmetRepository;
    private final TerminRepository terminRepository;
    private final GrupaRepository grupaRepository;
    private final RadRepository radRepository;
    private final StudentRepository studentRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final ZadatakRepository zadatakRepository;

    public boolean profesorPredajePredmet(Authentication auth, Long predmetId) {
        return imaUlogu(auth, "PROFESOR") && predmetId != null
                && predajePredmetRepository.getPredmetiZaPredavaca(auth.getName()).stream()
                .anyMatch(veza -> veza.getPredmet() != null
                        && predmetId.equals(veza.getPredmet().getId()));
    }

    public boolean profesorPredajePredmetTermina(Authentication auth, Long terminId) {
        return imaUlogu(auth, "PROFESOR") && terminId != null
                && terminRepository.findById(terminId)
                .map(Termin::getPredmet)
                .map(predmet -> profesorPredajePredmet(auth, predmet.getId()))
                .orElse(false);
    }

    public boolean profesorPredajePredmetGrupe(Authentication auth, Long grupaId) {
        return imaUlogu(auth, "PROFESOR") && grupaId != null
                && grupaRepository.findById(grupaId)
                .map(Grupa::getTermin)
                .map(Termin::getPredmet)
                .map(predmet -> profesorPredajePredmet(auth, predmet.getId()))
                .orElse(false);
    }

    public boolean profesorPredajePredmetRada(Authentication auth, Long radId) {
        return imaUlogu(auth, "PROFESOR") && radId != null
                && radRepository.findById(radId)
                .map(Rad::getZadatak)
                .map(Zadatak::getGrupa)
                .map(Grupa::getTermin)
                .map(Termin::getPredmet)
                .map(predmet -> profesorPredajePredmet(auth, predmet.getId()))
                .orElse(false);
    }

    public boolean studentJeVlasnikRada(Authentication auth, Long radId) {
        return imaUlogu(auth, "STUDENT") && radId != null
                && radRepository.findById(radId)
                .map(Rad::getStudent)
                .map(student -> auth.getName().equals(student.getEmail()))
                .orElse(false);
    }

    public boolean studentPripadaGrupi(Authentication auth, Long grupaId) {
        return imaUlogu(auth, "STUDENT") && grupaId != null
                && studentRepository.pronadjiPoEmailu(auth.getName())
                .map(Student::getId)
                .map(studentId -> prijavljenStudentRepository
                        .existsByStudent_IdAndGrupa_Id(studentId, grupaId))
                .orElse(false);
    }

    public boolean studentImaPristupZadatku(Authentication auth, Long zadatakId) {
        return imaUlogu(auth, "STUDENT") && zadatakId != null
                && zadatakRepository.findById(zadatakId)
                .map(Zadatak::getGrupa)
                .map(grupa -> studentPripadaGrupi(auth, grupa.getId()))
                .orElse(false);
    }

    private boolean imaUlogu(Authentication auth, String uloga) {
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getName() != null && !auth.getName().isBlank()
                && auth.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + uloga).equals(authority.getAuthority()));
    }
}
