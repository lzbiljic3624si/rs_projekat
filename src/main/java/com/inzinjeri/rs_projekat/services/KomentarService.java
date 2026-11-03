
package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.model.Komentar;
import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.repositories.KomentarRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KomentarService {

    private final KomentarRepository komentarRepository;
    private final RadRepository radRepository;
    private final PristupService pristupService;

    // Dodavanje komentara na rad
    public Komentar dodaj(
            Long radId,
            String tekstKomentara,
            String putanjaFajla,
            Integer brojLinije,
            Authentication auth) {

        Rad rad = radRepository.findById(radId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rad ne postoji"));

        // Proveravamo da li je korisnik profesor
        proveriProfesora(auth, radId);


        if (tekstKomentara == null || tekstKomentara.isBlank()) {
            throw new BadRequestException("Tekst komentara je obavezan");
        }

        if (tekstKomentara.length() > 2000) {
            throw new BadRequestException(
                    "Komentar ne moze biti duzi od 2000 karaktera");
        }


        if (putanjaFajla == null || putanjaFajla.isBlank()) {
            throw new BadRequestException("Putanja fajla je obavezna");
        }

        if (putanjaFajla.length() > 500) {
            throw new BadRequestException(
                    "Putanja fajla ne moze biti duza od 500 karaktera");
        }

        // Provera broja linije
        if (brojLinije == null || brojLinije <= 0) {
            throw new BadRequestException(
                    "Broj linije mora biti pozitivan broj");
        }

        // Kreiranje komentara
        Komentar komentar = new Komentar();
        komentar.setKomentar(tekstKomentara.trim());
        komentar.setRad(rad);
        komentar.setPutanjaFajla(putanjaFajla.trim());
        komentar.setBrojLinije(brojLinije);

        return komentarRepository.save(komentar);
    }

    // Prikaz komentara za odredjeni rad
    @Transactional(readOnly = true)
    public List<Komentar> zaRad(Long radId, Authentication auth) {

        radRepository.findById(radId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rad ne postoji"));

        // Komentare mogu da vide vlasnik rada ili profesor koji predaje predmet.
        boolean studentVlasnik =
                pristupService.studentJeVlasnikRada(auth, radId);

        boolean profesorPredaje =
                pristupService.profesorPredajePredmetRada(auth, radId);

        if (!studentVlasnik && !profesorPredaje) {
            throw new ForbiddenException(
                    "Nemate dozvolu da pregledate komentare ovog rada");
        }

        return komentarRepository
                .findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(radId);
    }

    // Brisanje komentara
    public void obrisi(Long komentarId, Authentication auth) {

        Komentar komentar = komentarRepository.findById(komentarId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Komentar ne postoji"));

        Long radId = komentar.getRad().getId();

        proveriProfesora(auth, radId);

        komentarRepository.delete(komentar);
    }

    // Provera uloge profesora i prava pristupa konkretnom radu.
    private void proveriProfesora(
            Authentication auth,
            Long radId) {

        boolean profesor = auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_PROFESOR".equals(authority.getAuthority()));

        if (!profesor) {
            throw new ForbiddenException(
                    "Samo profesor moze da izvrsi ovu operaciju");
        }

        if (!pristupService.profesorPredajePredmetRada(auth, radId)) {
            throw new ForbiddenException(
                    "Profesor ne predaje predmet kojem pripada ovaj rad");
        }
    }
}
