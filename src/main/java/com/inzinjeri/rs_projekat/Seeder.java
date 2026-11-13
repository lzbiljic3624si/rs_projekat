package com.inzinjeri.rs_projekat;

import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class Seeder implements CommandLineRunner {

    private final UcionicaRepository ucionicaRepository;
    private final PredmetRepository predmetRepository;
    private final RokRepository rokRepository;
    private final TerminRepository terminRepository;
    private final GrupaRepository grupaRepository;
    private final ProfesorRepository profesorRepository;
    private final StudentRepository studentRepository;
    private final PredajePredmetRepository predajePredmetRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final ZadatakRepository zadatakRepository;
    private final RadRepository radRepository;
    private final KomentarRepository komentarRepository;
    private final PasswordEncoder passwordEncoder;
    private final KorisnikRepository korisnikRepository;

    @Override
    public void run(String... args) throws Exception {
        // Postojecu bazu dopunjujemo nalozima i pocetnim komentarima.
        if (studentRepository.count() > 0) {
            napraviKorisnike(
                    studentRepository.findAll(),
                    profesorRepository.findAll()
            );
            napraviKomentare();
            return;
        }

        String lozinka = passwordEncoder.encode("lozinka123");

        String[] ucionice = {"RAF 1", "RAF 2", "RAF 3", "RAF 6", "Lab 1"};
        List<Ucionica> ucionicaList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Ucionica u = new Ucionica();
            u.setNaziv(ucionice[i - 1]);
            ucionicaList.add(ucionicaRepository.save(u));
        }

        // {naziv, jezik}
        String[][] predmeti = {
                {"Uvod u programiranje", "C"},
                {"Objektno orijentisano programiranje", "Java"},
                {"Strukture podataka i algoritmi", "C"},
                {"Operativni sistemi", "C"},
                {"Razvoj softvera", "Java"},
        };
        List<Predmet> predmetList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Predmet p = new Predmet();
            p.setNaziv(predmeti[i - 1][0]);
            p.setJezik(predmeti[i - 1][1]);
            predmetList.add(predmetRepository.save(p));
        }

        String[] rokovi = {"Januarski", "Februarski", "Junski", "Septembarski", "Novembarski"};
        int[] meseciRokova = {1, 2, 6, 9, 11};
        List<Rok> rokList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Rok r = new Rok();
            r.setNaziv(rokovi[i - 1]);
            r.setSkolskaGodina(i == 5 ? 2026 : 2025);
            rokList.add(rokRepository.save(r));
        }

        List<Termin> terminList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Termin t = new Termin();
            t.setDatum(LocalDate.of(2026, meseciRokova[i - 1], i == 5 ? 14 : 10 + i));
            t.setVremePocetak(LocalTime.of(i == 5 ? 12 : 8 + i, 0));
            t.setVremeKraj(LocalTime.of(i == 5 ? 14 : 10 + i, 0));
            t.setUcionica(ucionicaList.get(i - 1));
            t.setPredmet(predmetList.get(i - 1));
            t.setRok(rokList.get(i - 1));
            terminList.add(terminRepository.save(t));
        }

        List<Grupa> grupaList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Grupa g = new Grupa();
            g.setNaziv("Grupa" + i);
            g.setTermin(terminList.get(i - 1));
            grupaList.add(grupaRepository.save(g));
        }

        // {ime, prezime, indeks predmeta u predmetList}
        Object[][] profesori = {
                {"Đorđe", "Petrović", 0},     // djpetrovic@raf.rs
                {"Ljiljana", "Marković", 1},  // ljmarkovic@raf.rs
                {"Njegoš", "Kovačević", 2},   // njkovacevic@raf.rs
                {"Milica", "Jovanović", 3},   // mjovanovic@raf.rs
                {"Dragan", "Stojanović", 4},  // dstojanovic@raf.rs
                {"Petar", "Stamenic", 4},    // pstamenic@raf.rs
                {"Mirko", "Miric", 1},       // mmiric@raf.rs
        };
        List<Profesor> profesorList = new ArrayList<>();
        for (int i = 1; i <= profesori.length; i++) {
            Profesor p = new Profesor();
            p.setIme((String) profesori[i - 1][0]);
            p.setPrezime((String) profesori[i - 1][1]);
            p.setEmail(ParseUtils.profesorEmail(p.getIme(), p.getPrezime(), email ->
                    profesorRepository.existsByEmailIgnoreCase(email)
                            || korisnikRepository.existsByEmailIgnoreCase(email)));
            p.setPasswordHash(lozinka);
            profesorList.add(profesorRepository.save(p));
        }

        // {ime, prezime, smer, broj indeksa, godina upisa}
        Object[][] studenti = {
                {"Marko", "Nikolić", "RN", 12, 2023},     // mnikolic1223rn@raf.rs
                {"Ana", "Đorđević", "RI", 7, 2023},       // adjordjevic723ri@raf.rs
                {"Luka", "Šarić", "RN", 45, 2024},        // lsaric4524rn@raf.rs
                {"Jelena", "Živković", "RI", 101, 2024},  // jzivkovic10124ri@raf.rs
                {"Ljubica", "Popović", "RN", 3, 2022},    // ljpopovic322rn@raf.rs
                {"Lazar", "Zbiljic", "SI", 36, 2024},    // lzbiljic3624si@raf.rs
                {"David", "Milovanovic", "SI", 29, 2024}, // dmilovanovic2924si@raf.rs
                {"Laza", "Lazarevic", "RI", 67, 2024},   // llazarevic6724ri@raf.rs
        };
        List<Student> studentList = new ArrayList<>();
        for (int i = 1; i <= studenti.length; i++) {
            Object[] st = studenti[i - 1];
            Student s = new Student();
            s.setIme((String) st[0]);
            s.setPrezime((String) st[1]);
            s.setSmer((String) st[2]);
            s.setBrIndeksa(st[3] + "/" + st[4]);
            s.setGodUpisa((Integer) st[4]);
            s.setEmail(ParseUtils.studentEmail(s.getIme(), s.getPrezime(), (Integer) st[3], s.getGodUpisa(), s.getSmer()));
            s.setPasswordHash(lozinka);
            studentList.add(studentRepository.save(s));
        }

        // Kreiranje korisnickih naloga
        napraviKorisnike(studentList, profesorList);

        for (int i = 1; i <= profesori.length; i++) {
            PredajePredmet pp = new PredajePredmet();
            pp.setProfesor(profesorList.get(i - 1));
            pp.setPredmet(predmetList.get((Integer) profesori[i - 1][2]));
            predajePredmetRepository.save(pp);
        }

        for (int i = 1; i <= 5; i++) {
            PrijavljenStudent ps = new PrijavljenStudent();
            ps.setStudent(studentList.get(i - 1));
            ps.setGrupa(grupaList.get(i - 1));
            prijavljenStudentRepository.save(ps);
        }

        for (Student student : List.of(studentList.get(5), studentList.get(6))) {
            PrijavljenStudent ps = new PrijavljenStudent();
            ps.setStudent(student);
            ps.setGrupa(grupaList.get(4));
            prijavljenStudentRepository.save(ps);
        }

        String[] zadaci = {"Kalkulator u C-u", "Biblioteka (OOP)", "AVL stablo", "Raspoređivač procesa", "REST API za studentsku službu"};
        List<Zadatak> zadatakList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Zadatak z = new Zadatak();
            z.setNaziv(zadaci[i - 1]);
            z.setGrupa(grupaList.get(i - 1));
            z.setRepoPutanja("repo/zadatak" + i);
            zadatakList.add(zadatakRepository.save(z));
        }

        for (int i = 1; i <= 5; i++) {
            Rad r = new Rad();
            r.setStudent(studentList.get(i - 1));
            r.setZadatak(zadatakList.get(i - 1));
            r.setVremePreuzimanja(LocalDateTime.of(2026, 1, 10 + i, 9, 0));
            r.setVremePredaje(i % 2 == 0 ? LocalDateTime.of(2026, 1, 10 + i, 11, 0) : null);
            Termin termin = zadatakList.get(i - 1).getGrupa().getTermin();
            r.setRepoPutanjaResenja(ParseUtils.repoPutanjaResenja(
                    studentList.get(i - 1).getEmail(), termin.getPredmet().getNaziv(), termin.getDatum()));
            r.setPoeni(i == 4 ? 85.0 : null);
            r.setStatus(i == 4 ? StatusRada.OCENJEN : i % 2 == 0 ? StatusRada.PREDAT : StatusRada.PREUZET);
            radRepository.save(r);
        }
        napraviKomentare();
    }

    private void napraviKomentare() {
        if (komentarRepository.count() > 0) {
            return;
        }

        // {tekst komentara, putanja fajla}
        String[][] komentari = {
                {"Dodaj proveru deljenja nulom.", "src/main.c"},
                {"Proveri da li knjiga postoji pre pozajmljivanja.", "src/Biblioteka.java"},
                {"Proveri balans stabla nakon brisanja cvora.", "src/avl.c"},
                {"Obradi slucaj kada nema spremnih procesa.", "src/rasporedjivac.c"},
                {"Dodaj validaciju ulaznih podataka.", "src/StudentController.java"}
        };
        List<Rad> radovi = radRepository.findAll(Sort.by("id"));
        for (int i = 0; i < Math.min(radovi.size(), komentari.length); i++) {
            Komentar k = new Komentar();
            k.setRad(radovi.get(i));
            k.setKomentar(komentari[i][0]);
            k.setPutanjaFajla(komentari[i][1]);
            k.setBrojLinije(11 + i);
            komentarRepository.save(k);
        }
    }

    private void napraviKorisnike(
            List<Student> studenti,
            List<Profesor> profesori
    ){
        // Kreiranje korisnickih naloga za studente
        for (Student student : studenti) {

            if (!korisnikRepository.existsByEmail(student.getEmail())) {

                Korisnik korisnik = new Korisnik();
                korisnik.setEmail(student.getEmail());
                korisnik.setPasswordHash(student.getPasswordHash());
                korisnik.setUloga(Uloga.STUDENT);
                korisnik.setStudent(student);

                korisnikRepository.save(korisnik);
            }
        }

        // Kreiranje korisnickih naloga za profesore
        for (Profesor profesor : profesori) {

            if (!korisnikRepository.existsByEmail(profesor.getEmail())) {

                Korisnik korisnik = new Korisnik();
                korisnik.setEmail(profesor.getEmail());
                korisnik.setPasswordHash(profesor.getPasswordHash());
                korisnik.setUloga(Uloga.PROFESOR);
                korisnik.setProfesor(profesor);

                korisnikRepository.save(korisnik);
            }
        }
    }
}
