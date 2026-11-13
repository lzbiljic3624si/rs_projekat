package com.inzinjeri.rs_projekat;

import com.inzinjeri.rs_projekat.model.Uloga;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.services.PristupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:seeder_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class SeederTest {

    @Autowired Seeder seeder;
    @Autowired KomentarRepository komentari;
    @Autowired RadRepository radovi;
    @Autowired StudentRepository studenti;
    @Autowired KorisnikRepository korisnici;
    @Autowired TerminRepository termini;
    @Autowired PredajePredmetRepository predavanja;
    @Autowired PrijavljenStudentRepository prijave;
    @Autowired PristupService pristup;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void ispitRazvojaSoftveraImaTrazeneNalogePrijaveIPravaPristupa() throws Exception {
        var termin = termini.findAll().stream()
                .filter(t -> t.getPredmet().getNaziv().equals("Razvoj softvera"))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 14), termin.getDatum());
        assertEquals(LocalTime.of(12, 0), termin.getVremePocetak());
        assertEquals(LocalTime.of(14, 0), termin.getVremeKraj());

        String[][] nalozi = {
                {"lzbiljic3624si@raf.rs", "Lazar", "Zbiljic", "STUDENT"},
                {"dmilovanovic2924si@raf.rs", "David", "Milovanovic", "STUDENT"},
                {"llazarevic6724ri@raf.rs", "Laza", "Lazarevic", "STUDENT"},
                {"pstamenic@raf.rs", "Petar", "Stamenic", "PROFESOR"},
                {"mmiric@raf.rs", "Mirko", "Miric", "PROFESOR"}
        };
        for (var podaci : nalozi) {
            var nalog = korisnici.findAll().stream()
                    .filter(k -> k.getEmail().equals(podaci[0])).findFirst().orElseThrow();
            assertEquals(Uloga.valueOf(podaci[3]), nalog.getUloga());
            assertTrue(passwordEncoder.matches("lozinka123", nalog.getPasswordHash()));
            assertEquals(podaci[1], nalog.getStudent() != null
                    ? nalog.getStudent().getIme() : nalog.getProfesor().getIme());
            assertEquals(podaci[2], nalog.getStudent() != null
                    ? nalog.getStudent().getPrezime() : nalog.getProfesor().getPrezime());
        }
        var prijavljeni = prijave.findByGrupa_Termin_Id(termin.getId()).stream()
                .map(p -> p.getStudent().getEmail()).toList();
        assertTrue(prijavljeni.contains("lzbiljic3624si@raf.rs"));
        assertTrue(prijavljeni.contains("dmilovanovic2924si@raf.rs"));
        assertFalse(prijavljeni.contains("llazarevic6724ri@raf.rs"));
        assertTrue(studenti.pronadjiPoEmailu("llazarevic6724ri@raf.rs").isPresent());

        var petar = new UsernamePasswordAuthenticationToken("pstamenic@raf.rs", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESOR")));
        var mirko = new UsernamePasswordAuthenticationToken("mmiric@raf.rs", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESOR")));
        assertTrue(pristup.profesorPredajePredmetTermina(petar, termin.getId()));
        assertFalse(pristup.profesorPredajePredmetTermina(mirko, termin.getId()));
        assertTrue(predavanja.getPredmetiZaPredavaca("mmiric@raf.rs").stream()
                .anyMatch(p -> p.getPredmet().getNaziv().equals("Objektno orijentisano programiranje")));

        long brojNaloga = korisnici.count();
        long brojPrijava = prijave.count();
        seeder.run();
        assertEquals(brojNaloga, korisnici.count());
        assertEquals(brojPrijava, prijave.count());
    }

    @Test
    void popunjavaPrazneKomentareKadaRadoviVecPostoje() throws Exception {
        var radIds = radovi.findAll().stream().map(r -> r.getId()).sorted().toList();
        komentari.deleteAll();
        komentari.flush();
        assertEquals(0, komentari.count());

        seeder.run();

        assertEquals(5, komentari.count());
        assertEquals(radIds, radovi.findAll().stream().map(r -> r.getId()).sorted().toList());
        for (var radId : radIds) {
            assertEquals(1, komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(radId).size());
        }
        var komentarIds = komentari.findAll().stream().map(k -> k.getId()).sorted().toList();
        seeder.run();
        assertEquals(komentarIds, komentari.findAll().stream().map(k -> k.getId()).sorted().toList());
    }

    @Test
    void pocetniRadoviImajuValidneKomentareBezDupliranjaPriPonovnomPokretanju() throws Exception {
        assertEquals(5, komentari.count());
        for (var rad : radovi.findAll()) {
            var lista = komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(rad.getId());
            assertEquals(1, lista.size());
            var komentar = lista.getFirst();
            assertFalse(komentar.getKomentar().isBlank());
            assertTrue(komentar.getKomentar().length() <= 2000);
            assertFalse(komentar.getPutanjaFajla().isBlank());
            assertTrue(komentar.getPutanjaFajla().length() <= 500);
            assertTrue(komentar.getBrojLinije() > 0);
        }

        var ids = komentari.findAll().stream().map(k -> k.getId()).toList();
        seeder.run();
        assertEquals(ids, komentari.findAll().stream().map(k -> k.getId()).toList());
    }
}
