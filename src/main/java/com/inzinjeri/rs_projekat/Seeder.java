package com.inzinjeri.rs_projekat;

import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
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

        List<Ucionica> ucionicaList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Ucionica u = new Ucionica();
            u.setNaziv("U" + i);
            ucionicaList.add(ucionicaRepository.save(u));
        }

        List<Predmet> predmetList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Predmet p = new Predmet();
            p.setNaziv("Predmet " + i);
            p.setJezik(i % 2 == 0 ? "Java" : "C");
            p.setUslov(i > 1 ? "Predmet " + (i - 1) : null);
            predmetList.add(predmetRepository.save(p));
        }

        List<Rok> rokList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Rok r = new Rok();
            r.setNaziv("Rok " + i);
            r.setSkolskaGodina(2025);
            rokList.add(rokRepository.save(r));
        }

        List<Termin> terminList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Termin t = new Termin();
            t.setDatum(LocalDate.of(2026, 1, 10 + i));
            t.setVremePocetak(LocalTime.of(8 + i, 0));
            t.setVremeKraj(LocalTime.of(10 + i, 0));
            t.setUcionica(ucionicaList.get(i - 1));
            t.setPredmet(predmetList.get(i - 1));
            t.setRok(rokList.get(i - 1));
            terminList.add(terminRepository.save(t));
        }

        List<Grupa> grupaList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Grupa g = new Grupa();
            g.setNaziv("Grupa " + i);
            g.setTermin(terminList.get(i - 1));
            grupaList.add(grupaRepository.save(g));
        }

        List<Profesor> profesorList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Profesor p = new Profesor();
            p.setIme("Profesor" + i);
            p.setPrezime("Prezime" + i);
            p.setEmail("profesor" + i + "@example.com");
            p.setPasswordHash(lozinka);
            profesorList.add(profesorRepository.save(p));
        }

        List<Student> studentList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Student s = new Student();
            s.setIme("Student" + i);
            s.setPrezime("Prezime" + i);
            s.setSmer(i % 2 == 0 ? "RN" : "RI");
            s.setBrIndeksa(i + "/2023");
            s.setGodUpisa(2023);
            s.setEmail("student" + i + "@example.com");
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
            ps.setStatus(i % 2 == 0 ? "PRIHVACEN" : "NA_CEKANJU");
            prijavljenStudentRepository.save(ps);
        }

        List<Zadatak> zadatakList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Zadatak z = new Zadatak();
            z.setNaziv("Zadatak " + i);
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
