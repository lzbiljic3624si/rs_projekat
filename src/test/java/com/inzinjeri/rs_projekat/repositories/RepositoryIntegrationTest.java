package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIntegrationTest {
    @Autowired EntityManager em;
    @Autowired KorisnikRepository korisnici;
    @Autowired StudentRepository studenti;
    @Autowired ProfesorRepository profesori;
    @Autowired PredmetRepository predmeti;
    @Autowired PredajePredmetRepository predavanja;
    @Autowired RokRepository rokovi;
    @Autowired UcionicaRepository ucionice;
    @Autowired TerminRepository termini;
    @Autowired GrupaRepository grupe;
    @Autowired PrijavljenStudentRepository prijave;
    @Autowired ZadatakRepository zadaci;
    @Autowired RadRepository radovi;

    @Test
    void naloziSePronalazePoEmailuIPripadajucimOsobama() {
        Student student = student("ana@test.rs", "2020/001");
        Profesor profesor = profesor("prof@test.rs");
        Korisnik nalog = new Korisnik();
        nalog.setEmail("nalog@test.rs");
        nalog.setPasswordHash("hash");
        nalog.setUloga(Uloga.STUDENT);
        nalog.setStudent(student);
        em.persist(nalog);
        Korisnik profesorskiNalog = new Korisnik();
        profesorskiNalog.setEmail("prof.nalog@test.rs");
        profesorskiNalog.setPasswordHash("hash");
        profesorskiNalog.setUloga(Uloga.PROFESOR);
        profesorskiNalog.setProfesor(profesor);
        em.persist(profesorskiNalog);

        assertEquals(student.getId(), studenti.pronadjiPoEmailu("ana@test.rs").orElseThrow().getId());
        assertEquals(student.getId(), studenti.pronadjiPoBrojuIndeksa("2020/001").orElseThrow().getId());
        assertTrue(studenti.existsByEmailIgnoreCase("ANA@TEST.RS"));
        assertFalse(studenti.existsByEmailIgnoreCase("nema@test.rs"));
        assertEquals(profesor.getId(), profesori.findByEmailIgnoreCase("PROF@TEST.RS").orElseThrow().getId());
        assertEquals(1, profesori.getProfesorByEmail("prof@test.rs").size());
        assertTrue(profesori.existsByEmailIgnoreCase("PROF@TEST.RS"));
        assertEquals(nalog.getId(), korisnici.findByEmail("nalog@test.rs").orElseThrow().getId());
        assertEquals(nalog.getId(), korisnici.findByStudent_Id(student.getId()).orElseThrow().getId());
        assertEquals(profesorskiNalog.getId(), korisnici.findByProfesor_Id(profesor.getId()).orElseThrow().getId());
        assertTrue(korisnici.existsByEmail("nalog@test.rs"));
        assertTrue(korisnici.existsByEmailIgnoreCase("NALOG@TEST.RS"));
        assertFalse(korisnici.existsByEmail("nema@test.rs"));
    }

    @Test
    void predmetiIPredavanjaSeFiltrirajuPoNazivuJezikuIProfesoru() {
        Predmet java = predmet("Java", "sr");
        Predmet python = predmet("Python", "en");
        Profesor profesor = profesor("prof@test.rs");
        Profesor drugi = profesor("drugi@test.rs");
        PredajePredmet veza = predavanje(profesor, java);
        predavanje(drugi, python);

        assertEquals(java.getId(), predmeti.getPredmetByNaziv("Java").getFirst().getId());
        assertEquals(java.getId(), predmeti.getPredmetByJezik("sr").getFirst().getId());
        assertTrue(predmeti.getPredmetByNaziv("Nema").isEmpty());
        assertEquals(veza.getId(), predavanja.getPredmetiZaPredavaca("prof@test.rs").getFirst().getId());
        assertEquals(veza.getId(), predavanja.getPredavaciPredmeta("Java").getFirst().getId());
        assertTrue(predavanja.existsByProfesor_Id(profesor.getId()));
        assertTrue(predavanja.existsByPredmet_Id(java.getId()));
        assertTrue(predavanja.existsByProfesor_IdAndPredmet_Id(profesor.getId(), java.getId()));
        assertFalse(predavanja.existsByProfesor_IdAndPredmet_Id(profesor.getId(), python.getId()));
        assertFalse(predavanja.existsByProfesor_IdAndPredmet_IdAndIdNot(profesor.getId(), java.getId(), veza.getId()));
        assertTrue(predavanja.existsByProfesor_IdAndPredmet_IdAndIdNot(profesor.getId(), java.getId(), -1L));
    }

    @Test
    void rasporedPrijaveIRokoviVracuTacnoPovezanePodatke() {
        Predmet predmet = predmet("Java", "sr");
        Predmet drugiPredmet = predmet("Python", "en");
        Rok rok = rok("Januar", 2026);
        Rok drugiRok = rok("Februar", 2026);
        Ucionica ucionica = ucionica("A1");
        Ucionica drugaUcionica = ucionica("B1");
        LocalDate datum = LocalDate.of(2026, 1, 20);
        Termin termin = termin(predmet, rok, ucionica, datum, LocalTime.of(10, 0), LocalTime.of(12, 0));
        termin(drugiPredmet, drugiRok, drugaUcionica, datum.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));
        Grupa grupa = grupa("G1", termin);
        Student student = student("ana@test.rs", "2020/001");
        PrijavljenStudent prijava = new PrijavljenStudent();
        prijava.setStudent(student);
        prijava.setGrupa(grupa);
        em.persist(prijava);

        assertEquals(rok.getId(), rokovi.pronadjiPoNazivu("Januar").orElseThrow().getId());
        assertTrue(rokovi.existsByNazivAndSkolskaGodina("Januar", 2026));
        assertFalse(rokovi.existsByNazivAndSkolskaGodina("Januar", 2025));
        assertEquals(ucionica.getId(), ucionice.pronadjiPoNazivu("A1").orElseThrow().getId());
        assertEquals(termin.getId(), termini.pronadjiPoDatumu(datum).getFirst().getId());
        assertEquals(termin.getId(), termini.pronadjiPoPredmetu(predmet.getId()).getFirst().getId());
        assertEquals(termin.getId(), termini.pronadjiPoRoku(rok.getId()).getFirst().getId());
        assertEquals(termin.getId(), termini.pronadjiPoUcionici(ucionica.getId()).getFirst().getId());
        assertEquals(termin.getId(), termini.pronadjiPoPredmetuIRoku(predmet.getId(), rok.getId()).getFirst().getId());
        assertTrue(termini.pronadjiPoPredmetuIRoku(predmet.getId(), drugiRok.getId()).isEmpty());
        assertTrue(termini.existsByPredmet_Id(predmet.getId()));
        assertTrue(termini.existsByRok_Id(rok.getId()));
        assertTrue(termini.existsByUcionica_Id(ucionica.getId()));
        assertTrue(termini.postojiPreklapanje(ucionica.getId(), datum, LocalTime.of(11, 0), LocalTime.of(13, 0)));
        assertFalse(termini.postojiPreklapanje(ucionica.getId(), datum, LocalTime.NOON, LocalTime.of(13, 0)));
        assertFalse(termini.postojiPreklapanjeOsim(termin.getId(), ucionica.getId(), datum, LocalTime.of(11, 0), LocalTime.of(13, 0)));
        assertTrue(grupe.existsByNaziv("G1"));
        assertTrue(grupe.existsByTermin_Id(termin.getId()));
        assertEquals(grupa.getId(), grupe.getGrupeZaTermin(termin.getId()).getFirst().getId());
        assertEquals(grupa.getId(), grupe.findByIdZaIzmenu(grupa.getId()).orElseThrow().getId());
        assertEquals(prijava.getId(), prijave.getStudentByEmail("ana@test.rs").getFirst().getId());
        assertEquals(prijava.getId(), prijave.findByGrupa_Id(grupa.getId()).getFirst().getId());
        assertTrue(prijave.existsByStudent_IdAndGrupa_Id(student.getId(), grupa.getId()));
        assertTrue(prijave.existsByGrupa_Id(grupa.getId()));
        assertTrue(prijave.existsByStudent_Id(student.getId()));
    }

    @Test
    void zadaciIRadoviSePronalazeKrozGrupuIPredmetIStatus() {
        Predmet predmet = predmet("Java", "sr");
        Termin termin = termin(predmet, rok("Januar", 2026), ucionica("A1"),
                LocalDate.of(2026, 1, 20), LocalTime.of(10, 0), LocalTime.NOON);
        Grupa grupa = grupa("G1", termin);
        Zadatak zadatak = new Zadatak();
        zadatak.setNaziv("Zadatak 1");
        zadatak.setRepoPutanja("https://example.test/zadatak-1");
        zadatak.setGrupa(grupa);
        em.persist(zadatak);
        Student student = student("ana@test.rs", "2020/001");
        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setStatus(StatusRada.PREDAT);
        em.persist(rad);

        assertEquals(zadatak.getId(), zadaci.pronadjiPoNazivu("Zadatak 1").orElseThrow().getId());
        assertEquals(zadatak.getId(), zadaci.pronadjiPoGrupi(grupa.getId()).orElseThrow().getId());
        assertEquals(zadatak.getId(), zadaci.findByIdZaIzmenu(zadatak.getId()).orElseThrow().getId());
        assertTrue(zadaci.existsByRepoPutanja("https://example.test/zadatak-1"));
        assertTrue(zadaci.existsByGrupa_Id(grupa.getId()));
        assertEquals(rad.getId(), radovi.findByIdZaIzmenu(rad.getId()).orElseThrow().getId());
        assertEquals(rad.getId(), radovi.getRadByStudentEmail("ana@test.rs").getFirst().getId());
        assertEquals(rad.getId(), radovi.findByPredmetIdAndStatus(predmet.getId(), StatusRada.PREDAT).getFirst().getId());
        assertEquals(rad.getId(), radovi.findByPredmetIdAndStatus(predmet.getId(), null).getFirst().getId());
        assertTrue(radovi.findByPredmetIdAndStatus(predmet.getId(), StatusRada.OCENJEN).isEmpty());
        assertTrue(radovi.existsByStudent_Id(student.getId()));
        assertTrue(radovi.existsByStudent_IdAndZadatak_Id(student.getId(), zadatak.getId()));
        assertTrue(radovi.existsByZadatak_Id(zadatak.getId()));
        assertTrue(radovi.existsByStudent_IdAndZadatak_Grupa_Id(student.getId(), grupa.getId()));
        assertTrue(radovi.existsByZadatak_Grupa_Id(grupa.getId()));
    }

    private Student student(String email, String indeks) {
        Student student = new Student();
        student.setIme("Ana"); student.setPrezime("Anić"); student.setEmail(email);
        student.setBrIndeksa(indeks); student.setSmer("RI"); student.setGodUpisa(2020);
        student.setPasswordHash("hash");
        em.persist(student);
        return student;
    }

    private Profesor profesor(String email) {
        Profesor profesor = new Profesor();
        profesor.setIme("Pera"); profesor.setPrezime("Perić"); profesor.setEmail(email);
        profesor.setPasswordHash("hash");
        em.persist(profesor);
        return profesor;
    }

    private Predmet predmet(String naziv, String jezik) {
        Predmet predmet = new Predmet();
        predmet.setNaziv(naziv); predmet.setJezik(jezik);
        em.persist(predmet);
        return predmet;
    }

    private PredajePredmet predavanje(Profesor profesor, Predmet predmet) {
        PredajePredmet veza = new PredajePredmet();
        veza.setProfesor(profesor); veza.setPredmet(predmet);
        em.persist(veza);
        return veza;
    }

    private Rok rok(String naziv, int godina) {
        Rok rok = new Rok();
        rok.setNaziv(naziv); rok.setSkolskaGodina(godina);
        em.persist(rok);
        return rok;
    }

    private Ucionica ucionica(String naziv) {
        Ucionica ucionica = new Ucionica();
        ucionica.setNaziv(naziv);
        em.persist(ucionica);
        return ucionica;
    }

    private Termin termin(Predmet predmet, Rok rok, Ucionica ucionica, LocalDate datum, LocalTime pocetak, LocalTime kraj) {
        Termin termin = new Termin();
        termin.setPredmet(predmet); termin.setRok(rok); termin.setUcionica(ucionica);
        termin.setDatum(datum); termin.setVremePocetak(pocetak); termin.setVremeKraj(kraj);
        em.persist(termin);
        return termin;
    }

    private Grupa grupa(String naziv, Termin termin) {
        Grupa grupa = new Grupa();
        grupa.setNaziv(naziv); grupa.setTermin(termin);
        em.persist(grupa);
        return grupa;
    }
}
