package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.controllers.request.ProfesorRequest;
import com.inzinjeri.rs_projekat.controllers.request.StudentRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class NalogService {
    // shortcut: zajednicka lozinka je za demonstraciju; pre produkcije uvesti pojedinacne lozinke.
    private static final String POCETNA_LOZINKA = "lozinka123";
    private static final Pattern INDEKS = Pattern.compile("([0-9]{1,6})/([0-9]{4})");

    private final StudentRepository studentRepository;
    private final ProfesorRepository profesorRepository;
    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;

    public Student dodajStudenta(StudentRequest request) {
        Matcher indeks = INDEKS.matcher(request.getBrIndeksa().trim());
        if (!indeks.matches() || Integer.parseInt(indeks.group(1)) == 0
                || Integer.parseInt(indeks.group(2)) != request.getGodUpisa()) {
            throw new BadRequestException("Indeks mora biti broj/godina (npr. 12/2023), sa godinom jednakom godUpisa");
        }
        int broj = Integer.parseInt(indeks.group(1));
        String brIndeksa = broj + "/" + request.getGodUpisa();
        if (studentRepository.pronadjiPoBrojuIndeksa(brIndeksa).isPresent()) {
            throw new ConflictException("Student sa tim indeksom vec postoji");
        }

        String email = ParseUtils.studentEmail(request.getIme(), request.getPrezime(),
                broj, request.getGodUpisa(), request.getSmer());
        if (email == null || email.length() > 254) {
            throw new BadRequestException("Nije moguce generisati email");
        }
        if (emailZauzet(email)) {
            throw new ConflictException("Email je vec zauzet");
        }

        Student student = new Student();
        student.setIme(request.getIme().trim());
        student.setPrezime(request.getPrezime().trim());
        student.setSmer(request.getSmer().toUpperCase(Locale.ROOT));
        student.setBrIndeksa(brIndeksa);
        student.setGodUpisa(request.getGodUpisa());
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(POCETNA_LOZINKA));

        try {
            student = studentRepository.saveAndFlush(student);
            Korisnik korisnik = new Korisnik();
            korisnik.setEmail(student.getEmail());
            korisnik.setPasswordHash(student.getPasswordHash());
            korisnik.setUloga(Uloga.STUDENT);
            korisnik.setStudent(student);
            korisnikRepository.saveAndFlush(korisnik);
            return student;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Indeks ili email je vec zauzet", e);
        }
    }

    public Profesor dodajProfesora(ProfesorRequest request) {
        String email = ParseUtils.profesorEmail(request.getIme(), request.getPrezime(), this::emailZauzet);
        if (email == null || email.length() > 254) {
            throw new BadRequestException("Nije moguce generisati email");
        }

        Profesor profesor = new Profesor();
        profesor.setIme(request.getIme().trim());
        profesor.setPrezime(request.getPrezime().trim());
        profesor.setEmail(email);
        profesor.setPasswordHash(passwordEncoder.encode(POCETNA_LOZINKA));

        try {
            profesor = profesorRepository.saveAndFlush(profesor);
            Korisnik korisnik = new Korisnik();
            korisnik.setEmail(profesor.getEmail());
            korisnik.setPasswordHash(profesor.getPasswordHash());
            korisnik.setUloga(Uloga.PROFESOR);
            korisnik.setProfesor(profesor);
            korisnikRepository.saveAndFlush(korisnik);
            return profesor;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email je vec zauzet", e);
        }
    }

    private boolean emailZauzet(String email) {
        return korisnikRepository.existsByEmailIgnoreCase(email)
                || profesorRepository.existsByEmailIgnoreCase(email)
                || studentRepository.existsByEmailIgnoreCase(email);
    }
}
