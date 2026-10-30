package com.inzinjeri.rs_projekat;

import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Baza se ne brise izmedju pokretanja (ddl-auto=update), pa bi ponovni seed pukao na unique kolonama
        if (studentRepository.count() > 0) {
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

        // {naziv, jezik}; uslov je prethodni predmet u listi
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
            p.setUslov(i > 1 ? predmeti[i - 2][0] : null);
            predmetList.add(predmetRepository.save(p));
        }

        // skolska 2025 = 2025/26, pa su svi rokovi u 2026.
        String[] rokovi = {"Januarski", "Februarski", "Junski", "Septembarski", "Oktobarski"};
        int[] meseciRokova = {1, 2, 6, 9, 10};
        List<Rok> rokList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Rok r = new Rok();
            r.setNaziv(rokovi[i - 1]);
            r.setSkolskaGodina(2025);
            rokList.add(rokRepository.save(r));
        }

        List<Termin> terminList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Termin t = new Termin();
            t.setDatum(LocalDate.of(2026, meseciRokova[i - 1], 10 + i));
            t.setVremePocetak(LocalTime.of(8 + i, 0));
            t.setVremeKraj(LocalTime.of(10 + i, 0));
            t.setUcionica(ucionicaList.get(i - 1));
            t.setPredmet(predmetList.get(i - 1));
            t.setRok(rokList.get(i - 1));
            terminList.add(terminRepository.save(t));
        }

        String[] grupe = {"101", "102", "201", "202", "301"};
        List<Grupa> grupaList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Grupa g = new Grupa();
            g.setNaziv(grupe[i - 1]);
            g.setTermin(terminList.get(i - 1));
            grupaList.add(grupaRepository.save(g));
        }

        // namerno imena sa dvoslovnim inicijalima (dj, lj, nj) da se vidi ParseUtils.profesorEmail
        String[][] profesori = {
                {"Đorđe", "Petrović"},     // djpetrovic@raf.rs
                {"Ljiljana", "Marković"},  // ljmarkovic@raf.rs
                {"Njegoš", "Kovačević"},   // njkovacevic@raf.rs
                {"Milica", "Jovanović"},   // mjovanovic@raf.rs
                {"Dragan", "Stojanović"},  // dstojanovic@raf.rs
        };
        List<Profesor> profesorList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Profesor p = new Profesor();
            p.setIme(profesori[i - 1][0]);
            p.setPrezime(profesori[i - 1][1]);
            p.setEmail(ParseUtils.profesorEmail(p.getIme(), p.getPrezime()));
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
        };
        List<Student> studentList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
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

        for (int i = 1; i <= 5; i++) {
            PredajePredmet pp = new PredajePredmet();
            pp.setProfesor(profesorList.get(i - 1));
            pp.setPredmet(predmetList.get(i - 1));
            predajePredmetRepository.save(pp);
        }

        for (int i = 1; i <= 5; i++) {
            PrijavljenStudent ps = new PrijavljenStudent();
            ps.setStudent(studentList.get(i - 1));
            ps.setGrupa(grupaList.get(i - 1));
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
            r.setRepoPutanjaResenja(i % 2 == 0 ? "repo/resenje" + i : null);
            r.setPoeni(i == 4 ? 85.0 : null);
            r.setStatus(i == 4 ? "OCENJEN" : i % 2 == 0 ? "PREDAT" : "PREUZET");
            radRepository.save(r);
        }
    }
}
