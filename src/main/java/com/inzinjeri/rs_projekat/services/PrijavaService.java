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
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import com.inzinjeri.rs_projekat.utils.VremeTermina;
import com.inzinjeri.rs_projekat.controllers.request.PrijavaEditRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PrijavaService {
    private final PrijavljenStudentRepository prijavljanStudentRepository;
    private final StudentRepository studentRepository;
    private final GrupaRepository grupaRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final RadRepository radRepository;
    private final Clock clock;

    @Transactional
    public PrijavljenStudent izmeni(Long id, PrijavaEditRequest request, Authentication auth) {
        PrijavljenStudent prijava = prijavaZaIzmenu(id, auth);
        Grupa nova = grupaRepository.findByIdZaIzmenu(request.getGrupaId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        predmetId(nova);
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_PROFESOR".equals(a.getAuthority()))) {
            proveriProfesora(auth, nova);
        }
        VremeTermina.proveriPrePocetka(nova.getTermin(), LocalDateTime.now(clock));
        if (!prijava.getGrupa().getId().equals(nova.getId()) && prijavljanStudentRepository
                .existsByStudent_IdAndGrupa_Id(prijava.getStudent().getId(), nova.getId())) {
            throw new ConflictException("Student je vec prijavljen u novu grupu");
        }
        prijava.setGrupa(nova);
        return prijavljanStudentRepository.saveAndFlush(prijava);
    }

    @Transactional
    public void obrisi(Long id, Authentication auth) {
        PrijavljenStudent prijava = prijavaZaIzmenu(id, auth);
        prijavljanStudentRepository.delete(prijava);
        prijavljanStudentRepository.flush();
    }

    private PrijavljenStudent prijavaZaIzmenu(Long id, Authentication auth) {
        PrijavljenStudent prijava = prijavljanStudentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prijava ne postoji"));
        if (prijava.getStudent() == null || prijava.getGrupa() == null) {
            throw new ConflictException("Prijava nema studenta ili grupu");
        }
        boolean vlasnik = auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))
                && auth.getName().equals(prijava.getStudent().getEmail());
        if (!vlasnik) {
            proveriProfesora(auth, prijava.getGrupa());
        }
        VremeTermina.proveriPrePocetka(prijava.getGrupa().getTermin(), LocalDateTime.now(clock));
        if (radRepository.existsByStudent_IdAndZadatak_Grupa_Id(prijava.getStudent().getId(), prijava.getGrupa().getId())) {
            throw new ConflictException("Prijava ima preuzet rad");
        }
        return prijava;
    }

    @Transactional
    public void odjaviSe(Long grupaId, Authentication auth) {
        var student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));
        var prijava = prijavljanStudentRepository.getStudentByEmail(auth.getName()).stream()
                .filter(p -> p.getGrupa() != null && grupaId.equals(p.getGrupa().getId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Prijava ne postoji"));
        VremeTermina.proveriPrePocetka(prijava.getGrupa().getTermin(), LocalDateTime.now(clock));
        if (radRepository.existsByStudent_IdAndZadatak_Grupa_Id(student.getId(), grupaId)) {
            throw new ConflictException("Odjava nije moguca nakon preuzimanja zadatka");
        }
        prijavljanStudentRepository.delete(prijava);
    }

    @Transactional
    public Long prijaviSe(Long grupaId, Authentication auth) {
        var student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));
        Grupa grupa = grupaRepository.findByIdZaIzmenu(grupaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        predmetId(grupa);

        if (prijavljanStudentRepository.existsByStudent_IdAndGrupa_Id(student.getId(), grupaId)) {
            throw new ConflictException("Student je vec prijavljen u grupu");
        }

        VremeTermina.proveriPrePocetka(grupa.getTermin(), LocalDateTime.now(clock));

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
