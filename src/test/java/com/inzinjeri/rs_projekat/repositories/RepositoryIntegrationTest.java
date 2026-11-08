package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.controllers.request.GrupaRequest;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.services.RasporedService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

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
    @Autowired KomentarRepository komentari;

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
        assertEquals(grupa.getId(), grupe.pronadjiIstoimeneZaPredmetIRok("G1", predmet.getId(),
                rok.getId(), null).getFirst().getId());
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
    void nazivGrupeMozeDaSePonoviNaDrugomPredmetuIliUDrugomRoku() {
        Predmet java = predmet("Java", "sr");
        Predmet python = predmet("Python", "sr");
        Rok januar = rok("Januar", 2026);
        Rok februar = rok("Februar", 2026);
        Ucionica lab = ucionica("Lab");
        Termin prvi = terminGrupe(java, januar, lab);
        Termin drugiPredmet = terminGrupe(python, januar, lab);
        Termin drugiRok = terminGrupe(java, februar, lab);
        Grupa postojeca = grupa("G1", prvi);
        Authentication auth = profesorZa(java, python);

        Long druga = rasporedService().dodajGrupu(zahtevZaGrupu("G1", drugiPredmet), auth);
        Long treca = rasporedService().dodajGrupu(zahtevZaGrupu("G1", drugiRok), auth);
        em.flush();
        em.clear();

        assertNotEquals(postojeca.getId(), druga);
        assertNotEquals(druga, treca);
        assertEquals(drugiPredmet.getId(), grupe.findById(druga).orElseThrow().getTermin().getId());
        assertEquals(drugiRok.getId(), grupe.findById(treca).orElseThrow().getTermin().getId());
    }

    @Test
    void istiPredmetIRokNeDozvoljavajuDupliNazivNiUDrugomTerminu() {
        Predmet predmet = predmet("Java", "sr");
        Rok rok = rok("Januar", 2026);
        Ucionica lab = ucionica("Lab");
        grupa("G1", terminGrupe(predmet, rok, lab));
        Termin drugiTermin = terminGrupe(predmet, rok, lab);
        Authentication auth = profesorZa(predmet);

        assertThrows(ConflictException.class,
                () -> rasporedService().dodajGrupu(zahtevZaGrupu("G1", drugiTermin), auth));
        assertTrue(grupe.getGrupeZaTermin(drugiTermin.getId()).isEmpty());
    }

    @Test
    void promenaNazivaGrupeDozvoljavaNazivSaDrugogPredmeta() {
        Predmet java = predmet("Java", "sr");
        Predmet python = predmet("Python", "sr");
        Rok rok = rok("Januar", 2026);
        Ucionica lab = ucionica("Lab");
        Termin termin = terminGrupe(java, rok, lab);
        Grupa grupa = grupa("G2", termin);
        grupa("G1", terminGrupe(python, rok, lab));
        Authentication auth = profesorZa(java);

        rasporedService().izmeniGrupu(grupa.getId(), zahtevZaGrupu("G1", termin), auth);
        em.flush();
        em.clear();

        assertEquals("G1", grupe.findById(grupa.getId()).orElseThrow().getNaziv());
    }

    @Test
    void premestanjeGrupeSaIstimNazivomProveravaNoviPredmetIRok() {
        Predmet java = predmet("Java", "sr");
        Predmet python = predmet("Python", "sr");
        Rok rok = rok("Januar", 2026);
        Ucionica lab = ucionica("Lab");
        Termin prvi = terminGrupe(java, rok, lab);
        Termin drugi = terminGrupe(python, rok, lab);
        Grupa grupa = grupa("G1", prvi);
        grupa("G1", drugi);
        Authentication auth = profesorZa(java, python);

        assertEquals(grupa.getId(), rasporedService()
                .izmeniGrupu(grupa.getId(), zahtevZaGrupu("G1", prvi), auth).getId());
        assertThrows(ConflictException.class, () -> rasporedService()
                .izmeniGrupu(grupa.getId(), zahtevZaGrupu("G1", drugi), auth));
        assertEquals(prvi.getId(), grupa.getTermin().getId());
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
        assertEquals(rad.getId(), radovi.findByPredmetIdAndFilters(predmet.getId(), null, null, null, null).getFirst().getId());
        assertEquals(rad.getId(), radovi.findByPredmetIdAndFilters(predmet.getId(), StatusRada.PREDAT,
                grupa.getId(), termin.getId(), zadatak.getId()).getFirst().getId());
        assertTrue(radovi.findByPredmetIdAndFilters(predmet.getId(), null, -1L, null, null).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(predmet.getId(), null, null, -1L, null).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(predmet.getId(), null, null, null, -1L).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(-1L, null, grupa.getId(), termin.getId(), zadatak.getId()).isEmpty());
        assertTrue(radovi.existsByStudent_Id(student.getId()));
        assertTrue(radovi.existsByStudent_IdAndZadatak_Id(student.getId(), zadatak.getId()));
        assertTrue(radovi.existsByZadatak_Id(zadatak.getId()));
        assertTrue(radovi.existsByStudent_IdAndZadatak_Grupa_Id(student.getId(), grupa.getId()));
        assertTrue(radovi.existsByZadatak_Grupa_Id(grupa.getId()));
    }

    @Test
    void filteriRadovaKombinujuPredmetStatusGrupuTerminIZadatakPresekom() {
        Predmet java = predmet("Java", "sr");
        Predmet python = predmet("Python", "sr");
        Rok rok = rok("Januar", 2026);
        Ucionica lab = ucionica("Lab");
        Termin prviTermin = termin(java, rok, lab, LocalDate.of(2026, 1, 20), LocalTime.of(10, 0), LocalTime.NOON);
        Termin drugiTermin = termin(java, rok, lab, LocalDate.of(2026, 1, 21), LocalTime.of(10, 0), LocalTime.NOON);
        Termin tudjiTermin = termin(python, rok, lab, LocalDate.of(2026, 1, 22), LocalTime.of(10, 0), LocalTime.NOON);
        Grupa prvaGrupa = grupa("G1", prviTermin);
        Grupa drugaGrupa = grupa("G2", prviTermin);
        Grupa trecaGrupa = grupa("G3", drugiTermin);
        Grupa tudjaGrupa = grupa("G1", tudjiTermin);
        Zadatak prviZadatak = zadatak(prvaGrupa);
        Zadatak drugiZadatak = zadatak(drugaGrupa);
        Zadatak treciZadatak = zadatak(trecaGrupa);
        Zadatak tudjiZadatak = zadatak(tudjaGrupa);
        Student ana = student("ana@test.rs", "2020/001");
        Student boris = student("boris@test.rs", "2020/002");
        Rad prviPredat = rad(ana, prviZadatak, StatusRada.PREDAT);
        Rad prviOcenjen = rad(boris, prviZadatak, StatusRada.OCENJEN);
        Rad drugiPreuzet = rad(ana, drugiZadatak, StatusRada.PREUZET);
        Rad drugiPredat = rad(boris, drugiZadatak, StatusRada.PREDAT);
        Rad treciPredat = rad(ana, treciZadatak, StatusRada.PREDAT);
        Rad treciOcenjen = rad(boris, treciZadatak, StatusRada.OCENJEN);
        Rad tudjiPredat = rad(ana, tudjiZadatak, StatusRada.PREDAT);
        Rad tudjiPreuzet = rad(boris, tudjiZadatak, StatusRada.PREUZET);
        em.flush();
        em.clear();

        assertEquals(List.of(prviPredat.getId(), prviOcenjen.getId(), drugiPreuzet.getId(),
                        drugiPredat.getId(), treciPredat.getId(), treciOcenjen.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), null, null, null, null)));
        assertEquals(List.of(prviPredat.getId(), drugiPredat.getId(), treciPredat.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), StatusRada.PREDAT, null, null, null)));
        assertEquals(List.of(prviOcenjen.getId(), treciOcenjen.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), StatusRada.OCENJEN, null, null, null)));
        assertEquals(List.of(drugiPreuzet.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), StatusRada.PREUZET, null, null, null)));
        assertEquals(List.of(prviPredat.getId(), prviOcenjen.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), null, prvaGrupa.getId(), null, null)));
        assertEquals(List.of(prviPredat.getId(), prviOcenjen.getId(), drugiPreuzet.getId(), drugiPredat.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), null, null, prviTermin.getId(), null)));
        assertEquals(List.of(treciPredat.getId(), treciOcenjen.getId()),
                radIds(radovi.findByPredmetIdAndFilters(java.getId(), null, null, null, treciZadatak.getId())));
        assertEquals(List.of(tudjiPredat.getId(), tudjiPreuzet.getId()),
                radIds(radovi.findByPredmetIdAndFilters(python.getId(), null, null, null, null)));

        assertEquals(List.of(drugiPredat.getId()), radIds(radovi.findByPredmetIdAndFilters(java.getId(),
                StatusRada.PREDAT, drugaGrupa.getId(), prviTermin.getId(), drugiZadatak.getId())));
        assertEquals(List.of(prviPredat.getId(), prviOcenjen.getId()), radIds(radovi.findByPredmetIdAndFilters(
                java.getId(), null, prvaGrupa.getId(), prviTermin.getId(), prviZadatak.getId())));
        assertEquals(List.of(treciPredat.getId()), radIds(radovi.findByPredmetIdAndFilters(java.getId(),
                StatusRada.PREDAT, null, drugiTermin.getId(), treciZadatak.getId())));

        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), null,
                prvaGrupa.getId(), drugiTermin.getId(), null).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), null,
                prvaGrupa.getId(), null, drugiZadatak.getId()).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), StatusRada.OCENJEN,
                drugaGrupa.getId(), prviTermin.getId(), drugiZadatak.getId()).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), StatusRada.PREDAT,
                tudjaGrupa.getId(), null, null).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), null,
                null, tudjiTermin.getId(), null).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), null,
                null, null, tudjiZadatak.getId()).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(java.getId(), StatusRada.PREDAT,
                tudjaGrupa.getId(), tudjiTermin.getId(), tudjiZadatak.getId()).isEmpty());
        assertTrue(radovi.findByPredmetIdAndFilters(-1L, null, null, null, null).isEmpty());
    }

    @Test
    void komentariSeCuvajuFiltrirajuPoRaduISortirajuPoFajluILiniji() {
        Rad rad = new Rad(); rad.setStatus(StatusRada.PREDAT); em.persist(rad);
        Rad drugiRad = new Rad(); drugiRad.setStatus(StatusRada.PREDAT); em.persist(drugiRad);
        Komentar poslednji = komentar(rad, "src/Z.java", 1, "Greska u drugom fajlu");
        Komentar drugi = komentar(rad, "src/A.java", 10, "Greska u desetoj liniji");
        Komentar prvi = komentar(rad, "src/A.java", 2, "Greska u drugoj liniji");
        komentar(drugiRad, "src/A.java", 1, "Komentar drugog rada");
        em.flush();
        em.clear();

        var lista = komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(rad.getId());
        assertEquals(java.util.List.of(prvi.getId(), drugi.getId(), poslednji.getId()),
                lista.stream().map(Komentar::getId).toList());
        Komentar procitan = komentari.findById(prvi.getId()).orElseThrow();
        assertEquals(rad.getId(), procitan.getRad().getId());
        assertEquals("Greska u drugoj liniji", procitan.getKomentar());
        assertEquals("src/A.java", procitan.getPutanjaFajla());
        assertEquals(2, procitan.getBrojLinije());
        assertEquals(1, komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(drugiRad.getId()).size());
        assertTrue(komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(-1L).isEmpty());

        komentari.delete(procitan);
        em.flush();
        em.clear();
        assertTrue(komentari.findById(prvi.getId()).isEmpty());
        assertEquals(2, komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(rad.getId()).size());
        assertTrue(radovi.findById(rad.getId()).isPresent());
    }

    @Test
    void bazaPrihvataMaksimalneDuzineKomentaraIPutanje() {
        Rad rad = new Rad(); rad.setStatus(StatusRada.PREDAT); em.persist(rad);
        Komentar komentar = komentar(rad, "p".repeat(500), 1, "x".repeat(2000));
        em.flush();
        em.clear();

        Komentar procitan = komentari.findById(komentar.getId()).orElseThrow();
        assertEquals("p".repeat(500), procitan.getPutanjaFajla());
        assertEquals("x".repeat(2000), procitan.getKomentar());
    }

    private Termin terminGrupe(Predmet predmet, Rok rok, Ucionica lab) {
        return termin(predmet, rok, lab, LocalDate.of(2026, 1, 20), LocalTime.of(10, 0), LocalTime.NOON);
    }

    private GrupaRequest zahtevZaGrupu(String naziv, Termin termin) {
        GrupaRequest request = new GrupaRequest();
        request.setNaziv(naziv);
        request.setTerminId(termin.getId());
        return request;
    }

    private Authentication profesorZa(Predmet... predmeti) {
        Profesor profesor = profesor("prof@test.rs");
        for (Predmet predmet : predmeti) {
            predavanje(profesor, predmet);
        }
        return new UsernamePasswordAuthenticationToken("prof@test.rs", "",
                List.of(new SimpleGrantedAuthority("ROLE_PROFESOR")));
    }

    private RasporedService rasporedService() {
        return new RasporedService(rokovi, ucionice, termini, grupe, predmeti, predavanja, radovi, prijave, zadaci,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/Belgrade")));
    }

    private Zadatak zadatak(Grupa grupa) {
        Zadatak zadatak = new Zadatak();
        zadatak.setNaziv("Zadatak grupe " + grupa.getId());
        zadatak.setRepoPutanja("https://example.test/zadatak-" + grupa.getId());
        zadatak.setGrupa(grupa);
        em.persist(zadatak);
        return zadatak;
    }

    private Rad rad(Student student, Zadatak zadatak, StatusRada status) {
        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setStatus(status);
        em.persist(rad);
        return rad;
    }

    private List<Long> radIds(List<Rad> radovi) {
        return radovi.stream().map(Rad::getId).sorted().toList();
    }

    private Komentar komentar(Rad rad, String putanja, int linija, String tekst) {
        Komentar komentar = new Komentar();
        komentar.setRad(rad); komentar.setPutanjaFajla(putanja);
        komentar.setBrojLinije(linija); komentar.setKomentar(tekst);
        return komentari.save(komentar);
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
