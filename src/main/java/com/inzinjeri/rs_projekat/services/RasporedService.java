package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.VremeTermina;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RasporedService {
    private final RokRepository rokRepository;
    private final UcionicaRepository ucionicaRepository;
    private final TerminRepository terminRepository;
    private final GrupaRepository grupaRepository;
    private final PredmetRepository predmetRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final RadRepository radRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final ZadatakRepository zadatakRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Rok> rokovi() {
        return rokRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Ucionica> ucionice() {
        return ucionicaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Termin> termini(Long predmetId, Long rokId) {
        return terminRepository.pronadjiPoPredmetuIRoku(predmetId, rokId);
    }

    @Transactional(readOnly = true)
    public List<Grupa> grupe(Long terminId) {
        return grupaRepository.getGrupeZaTermin(terminId);
    }

    @Transactional(readOnly = true)
    public Rok rok(Long id) {
        return rokRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rok ne postoji"));
    }

    @Transactional(readOnly = true)
    public Ucionica ucionica(Long id) {
        return ucionicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ucionica ne postoji"));
    }

    @Transactional(readOnly = true)
    public Termin termin(Long id) {
        return terminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
    }

    @Transactional(readOnly = true)
    public Grupa grupa(Long id) {
        return grupaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
    }

    @Transactional
    public Rok izmeniRok(Long id, RokRequest request, Authentication auth) {
        proveriProfesora(auth);
        Rok rok = rok(id);
        if ((!request.getNaziv().equals(rok.getNaziv())
                || !request.getSkolskaGodina().equals(rok.getSkolskaGodina()))
                && rokRepository.existsByNazivAndSkolskaGodina(request.getNaziv(), request.getSkolskaGodina())) {
            throw new ConflictException("Rok vec postoji");
        }
        rok.setNaziv(request.getNaziv());
        rok.setSkolskaGodina(request.getSkolskaGodina());
        return rokRepository.save(rok);
    }

    @Transactional
    public Ucionica izmeniUcionicu(Long id, UcionicaRequest request, Authentication auth) {
        proveriProfesora(auth);
        Ucionica ucionica = ucionica(id);
        if (ucionicaRepository.pronadjiPoNazivu(request.getNaziv())
                .filter(postojeca -> !id.equals(postojeca.getId())).isPresent()) {
            throw new ConflictException("Ucionica vec postoji");
        }
        ucionica.setNaziv(request.getNaziv());
        return ucionicaRepository.save(ucionica);
    }

    @Transactional
    public void obrisiRok(Long id, Authentication auth) {
        proveriProfesora(auth);
        Rok rok = rok(id);
        if (terminRepository.existsByRok_Id(id)) {
            throw new ConflictException("Rok ima termine");
        }
        rokRepository.delete(rok);
    }

    @Transactional
    public void obrisiUcionicu(Long id, Authentication auth) {
        proveriProfesora(auth);
        Ucionica ucionica = ucionica(id);
        if (terminRepository.existsByUcionica_Id(id)) {
            throw new ConflictException("Ucionica ima termine");
        }
        ucionicaRepository.delete(ucionica);
    }

    @Transactional
    public void obrisiTermin(Long id, Authentication auth) {
        Termin termin = termin(id);
        if (termin.getPredmet() == null) {
            throw new ConflictException("Termin nema predmet");
        }
        proveriDaPredaje(auth, termin.getPredmet().getId());
        if (grupaRepository.existsByTermin_Id(id)) {
            throw new ConflictException("Termin ima grupe");
        }
        terminRepository.delete(termin);
    }

    @Transactional
    public void obrisiGrupu(Long id, Authentication auth) {
        Grupa grupa = grupaRepository.findByIdZaIzmenu(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        if (grupa.getTermin() == null || grupa.getTermin().getPredmet() == null) {
            throw new ConflictException("Grupa nema predmet");
        }
        proveriDaPredaje(auth, grupa.getTermin().getPredmet().getId());
        if (prijavljenStudentRepository.existsByGrupa_Id(id)
                || zadatakRepository.existsByGrupa_Id(id)
                || radRepository.existsByZadatak_Grupa_Id(id)) {
            throw new ConflictException("Grupa ima prijave, zadatak ili radove");
        }
        grupaRepository.delete(grupa);
    }

    @Transactional
    public Long dodajRok(RokRequest request, Authentication auth) {
        proveriProfesora(auth);

        if (rokRepository.existsByNazivAndSkolskaGodina(
                request.getNaziv(), request.getSkolskaGodina())) {
            throw new ConflictException("Rok vec postoji");
        }

        Rok rok = new Rok();
        rok.setNaziv(request.getNaziv());
        rok.setSkolskaGodina(request.getSkolskaGodina());

        return rokRepository.save(rok).getId();
    }

    @Transactional
    public Long dodajUcionicu(UcionicaRequest request, Authentication auth) {
        proveriProfesora(auth);

        if (ucionicaRepository.pronadjiPoNazivu(request.getNaziv()).isPresent()) {
            throw new ConflictException("Ucionica vec postoji");
        }

        Ucionica ucionica = new Ucionica();
        ucionica.setNaziv(request.getNaziv());

        return ucionicaRepository.save(ucionica).getId();
    }

    @Transactional
    public Long dodajTermin(TerminRequest request, Authentication auth) {
        return terminRepository.save(pripremiTermin(request, auth, null)).getId();
    }

    @Transactional
    public Termin izmeniTermin(Long id, TerminRequest request, Authentication auth) {
        Termin termin = terminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
        if (termin.getPredmet() == null || termin.getRok() == null) {
            throw new ConflictException("Termin nema predmet ili rok");
        }
        proveriDaPredaje(auth, termin.getPredmet().getId());
        VremeTermina.proveriPrePocetka(termin, LocalDateTime.now(clock));
        if (!termin.getPredmet().getId().equals(request.getPredmetId())
                || !termin.getRok().getId().equals(request.getRokId())) {
            throw new ConflictException("Predmet i rok postojeceg termina se ne menjaju");
        }
        Termin izmena = pripremiTermin(request, auth, id);
        termin.setDatum(izmena.getDatum());
        termin.setVremePocetak(izmena.getVremePocetak());
        termin.setVremeKraj(izmena.getVremeKraj());
        termin.setUcionica(izmena.getUcionica());
        return terminRepository.save(termin);
    }

    private Termin pripremiTermin(TerminRequest request, Authentication auth, Long izuzetiId) {
        if (!request.getVremePocetak().isBefore(request.getVremeKraja())) {
            throw new BadRequestException("Pocetak termina mora biti pre kraja");
        }

        Predmet predmet = predmetRepository.findById(request.getPredmetId())
                .orElseThrow(() -> new ResourceNotFoundException("Predmet ne postoji"));

        proveriDaPredaje(auth, predmet.getId());

        Ucionica ucionica = ucionicaRepository.findById(
                        request.getUcionicaId())
                .orElseThrow(() -> new ResourceNotFoundException("Ucionica ne postoji"));

        Rok rok = rokRepository.findById(request.getRokId())
                .orElseThrow(() -> new ResourceNotFoundException("Rok ne postoji"));

        boolean preklapanje = izuzetiId == null
                ? terminRepository.postojiPreklapanje(ucionica.getId(), request.getDatum(),
                        request.getVremePocetak(), request.getVremeKraja())
                : terminRepository.postojiPreklapanjeOsim(izuzetiId, ucionica.getId(), request.getDatum(),
                        request.getVremePocetak(), request.getVremeKraja());
        if (preklapanje) {
            throw new ConflictException("Učionica je zauzeta u tom terminu");
        }

        Termin termin = new Termin();
        termin.setDatum(request.getDatum());
        termin.setVremePocetak(request.getVremePocetak());
        termin.setVremeKraj(request.getVremeKraja());
        termin.setUcionica(ucionica);
        termin.setPredmet(predmet);
        termin.setRok(rok);

        VremeTermina.proveriPrePocetka(termin, LocalDateTime.now(clock));
        return termin;
    }

    @Transactional
    public Long dodajGrupu(GrupaRequest request, Authentication auth) {
        Termin termin = terminRepository.findById(request.getTerminId())
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));

        if (termin.getPredmet() == null) {
            throw new ConflictException("Termin nema predmet");
        }

        proveriDaPredaje(auth, termin.getPredmet().getId());

        if (grupaRepository.existsByNaziv(request.getNaziv())) {
            throw new ConflictException("Grupa već postoji");
        }

        Grupa grupa = new Grupa();
        grupa.setNaziv(request.getNaziv());
        grupa.setTermin(termin);

        return grupaRepository.save(grupa).getId();

    }

    @Transactional
    public Grupa izmeniGrupu(Long id, GrupaRequest request, Authentication auth) {
        Grupa grupa = grupaRepository.findByIdZaIzmenu(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji"));
        if (grupa.getTermin() == null || grupa.getTermin().getPredmet() == null) {
            throw new ConflictException("Grupa nema predmet");
        }
        proveriDaPredaje(auth, grupa.getTermin().getPredmet().getId());
        VremeTermina.proveriPrePocetka(grupa.getTermin(), LocalDateTime.now(clock));
        Termin termin = terminRepository.findById(request.getTerminId())
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
        if (termin.getPredmet() == null) {
            throw new ConflictException("Termin nema predmet");
        }
        proveriDaPredaje(auth, termin.getPredmet().getId());
        VremeTermina.proveriPrePocetka(termin, LocalDateTime.now(clock));
        if (radRepository.existsByZadatak_Grupa_Id(id)) {
            throw new ConflictException("Grupa ima preuzete radove");
        }
        if (!grupa.getTermin().getId().equals(termin.getId()) && prijavljenStudentRepository.existsByGrupa_Id(id)) {
            throw new ConflictException("Grupa sa prijavama ne moze da promeni termin");
        }
        if (!request.getNaziv().equals(grupa.getNaziv()) && grupaRepository.existsByNaziv(request.getNaziv())) {
            throw new ConflictException("Grupa vec postoji");
        }
        grupa.setNaziv(request.getNaziv());
        grupa.setTermin(termin);
        return grupaRepository.save(grupa);
    }

    private void proveriProfesora(Authentication auth) {
        boolean profesor = auth.getAuthorities().stream()
                .anyMatch(uloga ->
                        "ROLE_PROFESOR".equals(uloga.getAuthority()));

        if (!profesor) {
            throw new ForbiddenException("Samo profesor može da menja raspored");
        }
    }

    private void proveriDaPredaje(
            Authentication auth, Long predmetId) {
        proveriProfesora(auth);

        boolean predaje = predajePredmetRepository
                .getPredmetiZaPredavaca(auth.getName())
                .stream()
                .anyMatch(veza ->
                        veza.getPredmet() != null
                                && predmetId.equals(
                                veza.getPredmet().getId()));

        if (!predaje) {
            throw new ForbiddenException("Profesor ne predaje ovaj predmet");
        }
    }
}
