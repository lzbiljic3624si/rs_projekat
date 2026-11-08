package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.OcenaRadaRequest;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.StatusRada;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.model.Zadatak;
import com.inzinjeri.rs_projekat.utils.VremeTermina;
import com.inzinjeri.rs_projekat.utils.ParseUtils;
import com.inzinjeri.rs_projekat.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RadService {

    private final RadRepository radRepository;
    private final StudentRepository studentRepository;
    private final ZadatakRepository zadatakRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Rad> zaPredmet(
            Long predmetId,
            StatusRada status,
            Long grupaId,
            Long terminId,
            Long zadatakId,
            Authentication auth) {

        proveriDaPredaje(auth, predmetId);

        return radRepository.findByPredmetIdAndFilters(
                predmetId,
                status,
                grupaId,
                terminId,
                zadatakId
        );
    }

    @Transactional(readOnly = true)
    public Rad byId(Long radId, Authentication auth) {
        Rad rad = radRepository.findById(radId)
                .orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
        proveriPristupRadu(rad, auth);
        return rad;
    }

    private void proveriPristupRadu(Rad rad, Authentication auth) {
        boolean vlasnik = auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))
                && rad.getStudent() != null && auth.getName().equals(rad.getStudent().getEmail());
        if (!vlasnik) {
            if (rad.getZadatak() == null || rad.getZadatak().getGrupa() == null
                    || rad.getZadatak().getGrupa().getTermin() == null
                    || rad.getZadatak().getGrupa().getTermin().getPredmet() == null) {
                throw new ConflictException("Rad nema predmet");
            }
            proveriDaPredaje(auth, rad.getZadatak().getGrupa().getTermin().getPredmet().getId());
        }
    }

    @Transactional
    public Rad izmeni(Long id, OcenaRadaRequest request, Authentication auth) {
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_PROFESOR".equals(a.getAuthority()))) {
            throw new ForbiddenException("Samo profesor moze da ispravlja poene rada");
        }
        Rad rad = radZaIzmenu(id);
        proveriPristupRadu(rad, auth);
        if (request.getPoeni() == null || !Double.isFinite(request.getPoeni()) || request.getPoeni() < 0) {
            throw new BadRequestException("Poeni moraju biti nenegativan broj");
        }
        if (rad.getStatus() != StatusRada.OCENJEN) {
            throw new ConflictException("Ispravka poena je moguca samo za ocenjen rad");
        }
        rad.setPoeni(request.getPoeni());
        return radRepository.save(rad);
    }

    @Transactional
    public void obrisi(Long id, Authentication auth) {
        Rad rad = radZaIzmenu(id);
        proveriPristupRadu(rad, auth);
        radRepository.delete(rad);
        radRepository.flush();
    }

    private Rad radZaIzmenu(Long id) {
        return radRepository.findByIdZaIzmenu(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
    }

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
        Rad rad = radZaIzmenu(radId);

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
    public Rad predaj(Long radId, Authentication auth){
        Rad rad = radZaIzmenu(radId);
        if(rad.getStudent() == null
                || !auth.getName().equals(rad.getStudent().getEmail())){
            throw new ForbiddenException("Ovaj rad nije tvoj");
        }
        if(rad.getStatus() != StatusRada.PREUZET){
            throw new ConflictException("Rad ne moze ponovo da se preda");
        }

        LocalDateTime sada = LocalDateTime.now(clock);
        VremeTermina.proveriPredaju(rad.getZadatak() == null || rad.getZadatak().getGrupa() == null
                ? null : rad.getZadatak().getGrupa().getTermin(), sada);

        if (rad.getRepoPutanjaResenja() == null || rad.getRepoPutanjaResenja().isBlank()) {
            rad.setRepoPutanjaResenja(automatskaPutanjaResenja(rad.getStudent(), rad.getZadatak()));
        }
        rad.setVremePredaje(sada);
        rad.setStatus(StatusRada.PREDAT);

        return radRepository.save(rad);
    }

    @Transactional
    public Long preuzmi(Long zadatakId, Authentication auth){
        Student student = studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Prijavljeni korisnik nije student"));

        Zadatak zadatak = zadatakRepository.findByIdZaIzmenu(zadatakId)
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

        LocalDateTime sada = LocalDateTime.now(clock);
        VremeTermina.proveriPreuzimanje(zadatak.getGrupa().getTermin(), sada);

        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setRepoPutanjaResenja(automatskaPutanjaResenja(student, zadatak));
        rad.setVremePreuzimanja(sada);
        rad.setStatus(StatusRada.PREUZET);

        return radRepository.save(rad).getId();
    }

    private String automatskaPutanjaResenja(Student student, Zadatak zadatak) {
        var termin = zadatak == null || zadatak.getGrupa() == null ? null : zadatak.getGrupa().getTermin();
        if (student == null || termin == null || termin.getPredmet() == null) {
            throw new ConflictException("Nedostaju podaci za putanju repozitorijuma resenja");
        }
        String putanja = ParseUtils.repoPutanjaResenja(student.getEmail(), termin.getPredmet().getNaziv(), termin.getDatum());
        if (putanja == null) {
            throw new ConflictException("Email, predmet i datum moraju omoguciti putanju resenja do 255 znakova");
        }
        return putanja;
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
