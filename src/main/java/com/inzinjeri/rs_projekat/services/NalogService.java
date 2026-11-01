package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.exceptions.ForbiddenException;
import com.inzinjeri.rs_projekat.exceptions.ResourceNotFoundException;
import com.inzinjeri.rs_projekat.controllers.request.NalogRequest;
import com.inzinjeri.rs_projekat.controllers.request.ProfesorRequest;
import com.inzinjeri.rs_projekat.controllers.request.StudentRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import com.inzinjeri.rs_projekat.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
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
    private final RadRepository radRepository;
    private final PrijavljenStudentRepository prijavljenStudentRepository;
    private final PredajePredmetRepository predajePredmetRepository;

    public Student dodajStudenta(StudentRequest request) {
        String brIndeksa = normalizujIndeks(request);
        int broj = Integer.parseInt(brIndeksa.substring(0, brIndeksa.indexOf('/')));
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

    @Transactional(readOnly = true)
    public List<Student> sviStudenti() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Student student(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student nije pronadjen"));
    }

    @Transactional(readOnly = true)
    public Student student(Long id, Authentication auth) {
        boolean profesor = auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PROFESOR".equals(authority.getAuthority()));
        if (!profesor && !mojStudent(auth).getId().equals(id)) {
            throw new ForbiddenException("Student moze da vidi samo svoj profil");
        }
        return student(id);
    }

    @Transactional(readOnly = true)
    public Student mojStudent(Authentication auth) {
        return studentRepository.pronadjiPoEmailu(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student nije pronadjen"));
    }

    public Student izmeniStudenta(Long id, StudentRequest request) {
        Student student = student(id);
        String indeks = normalizujIndeks(request);
        if (studentRepository.pronadjiPoBrojuIndeksa(indeks)
                .filter(drugi -> !id.equals(drugi.getId())).isPresent()) {
            throw new ConflictException("Student sa tim indeksom vec postoji");
        }
        student.setIme(request.getIme().trim());
        student.setPrezime(request.getPrezime().trim());
        student.setSmer(request.getSmer().toUpperCase(Locale.ROOT));
        student.setBrIndeksa(indeks);
        student.setGodUpisa(request.getGodUpisa());
        return studentRepository.saveAndFlush(student);
    }

    public void obrisiStudenta(Long id) {
        Student student = student(id);
        if (radRepository.existsByStudent_Id(id) || prijavljenStudentRepository.existsByStudent_Id(id)) {
            throw new ConflictException("Student ima radove ili prijave");
        }
        korisnikRepository.findByStudent_Id(id).ifPresent(korisnikRepository::delete);
        korisnikRepository.flush();
        studentRepository.delete(student);
        studentRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<Profesor> sviProfesori() {
        return profesorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Profesor profesor(Long id) {
        return profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor nije pronadjen"));
    }

    @Transactional(readOnly = true)
    public Profesor mojProfesor(Authentication auth) {
        return profesorRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor nije pronadjen"));
    }

    public Profesor izmeniProfesora(Long id, ProfesorRequest request) {
        Profesor profesor = profesor(id);
        profesor.setIme(request.getIme().trim());
        profesor.setPrezime(request.getPrezime().trim());
        return profesorRepository.saveAndFlush(profesor);
    }

    public void obrisiProfesora(Long id) {
        Profesor profesor = profesor(id);
        if (predajePredmetRepository.existsByProfesor_Id(id)) {
            throw new ConflictException("Profesor je dodeljen predmetima");
        }
        korisnikRepository.findByProfesor_Id(id).ifPresent(korisnikRepository::delete);
        korisnikRepository.flush();
        profesorRepository.delete(profesor);
        profesorRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<Korisnik> sviNalozi() {
        return korisnikRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Korisnik nalog(Long id) {
        return korisnikRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nalog nije pronadjen"));
    }

    @Transactional(readOnly = true)
    public Korisnik mojNalog(Authentication auth) {
        return korisnikRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Nalog nije pronadjen"));
    }

    public Korisnik izmeniNalog(Long id, NalogRequest request) {
        Korisnik nalog = nalog(id);
        String email = request.getEmail().trim();
        if (!email.equalsIgnoreCase(nalog.getEmail()) && emailZauzet(email)) {
            throw new ConflictException("Email je vec zauzet");
        }
        nalog.setEmail(email);
        if (nalog.getStudent() != null) {
            nalog.getStudent().setEmail(email);
        }
        if (nalog.getProfesor() != null) {
            nalog.getProfesor().setEmail(email);
        }
        return korisnikRepository.saveAndFlush(nalog);
    }

    public void obrisiNalog(Long id) {
        korisnikRepository.delete(nalog(id));
        korisnikRepository.flush();
    }

    private String normalizujIndeks(StudentRequest request) {
        Matcher indeks = INDEKS.matcher(request.getBrIndeksa().trim());
        if (!indeks.matches() || Integer.parseInt(indeks.group(1)) == 0
                || Integer.parseInt(indeks.group(2)) != request.getGodUpisa()) {
            throw new BadRequestException("Indeks mora biti broj/godina (npr. 12/2023), sa godinom jednakom godUpisa");
        }
        return Integer.parseInt(indeks.group(1)) + "/" + request.getGodUpisa();
    }

    private boolean emailZauzet(String email) {
        return korisnikRepository.existsByEmailIgnoreCase(email)
                || profesorRepository.existsByEmailIgnoreCase(email)
                || studentRepository.existsByEmailIgnoreCase(email);
    }
}
