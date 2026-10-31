package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.controllers.request.ProfesorRequest;
import com.inzinjeri.rs_projekat.controllers.request.StudentRequest;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NalogServiceTest {
    private final StudentRepository studenti = mock(StudentRepository.class);
    private final ProfesorRepository profesori = mock(ProfesorRepository.class);
    private final KorisnikRepository korisnici = mock(KorisnikRepository.class);
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final NalogService service = new NalogService(studenti, profesori, korisnici, encoder);

    @Test
    void studentDobijaGenerisaniEmailIUpotrebljivPovezanNalog() {
        when(studenti.saveAndFlush(any())).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(7L);
            return student;
        });

        Student student = service.dodajStudenta(studentRequest());

        assertEquals("mnikolic1223rn@raf.rs", student.getEmail());
        assertEquals("RN", student.getSmer());
        assertEquals("12/2023", student.getBrIndeksa());
        assertTrue(encoder.matches("lozinka123", student.getPasswordHash()));
        Korisnik korisnik = sacuvaniKorisnik();
        assertSame(student, korisnik.getStudent());
        assertNull(korisnik.getProfesor());
        assertEquals(Uloga.STUDENT, korisnik.getUloga());
        assertEquals(student.getEmail(), korisnik.getEmail());
        assertEquals(student.getPasswordHash(), korisnik.getPasswordHash());
    }

    @Test
    void profesorPreskaceAdreseZauzeteUProfilimaIKorisnicima() {
        when(profesori.existsByEmailIgnoreCase("mpetrovic@raf.rs")).thenReturn(true);
        when(korisnici.existsByEmailIgnoreCase("mapetrovic@raf.rs")).thenReturn(true);
        when(studenti.existsByEmailIgnoreCase("marpetrovic@raf.rs")).thenReturn(true);
        when(profesori.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Profesor profesor = service.dodajProfesora(profesorRequest());

        assertEquals("maripetrovic@raf.rs", profesor.getEmail());
        assertTrue(encoder.matches("lozinka123", profesor.getPasswordHash()));
        Korisnik korisnik = sacuvaniKorisnik();
        assertSame(profesor, korisnik.getProfesor());
        assertNull(korisnik.getStudent());
        assertEquals(Uloga.PROFESOR, korisnik.getUloga());
        assertEquals(profesor.getEmail(), korisnik.getEmail());
        assertEquals(profesor.getPasswordHash(), korisnik.getPasswordHash());
    }

    @Test
    void duplikatIndeksaSeOdbijaPreCuvanja() {
        when(studenti.pronadjiPoBrojuIndeksa("12/2023")).thenReturn(Optional.of(new Student()));
        assertThrows(ConflictException.class, () -> service.dodajStudenta(studentRequest()));
        verify(studenti, never()).saveAndFlush(any());
        verify(korisnici, never()).saveAndFlush(any());
    }

    @Test
    void duplikatStudentskogEmailaSeOdbija() {
        when(korisnici.existsByEmailIgnoreCase("mnikolic1223rn@raf.rs")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.dodajStudenta(studentRequest()));
        verify(studenti, never()).saveAndFlush(any());
        verify(korisnici, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12/2024", "12", "0/2023", "12/23", "abc/2023", "1234567/2023"})
    void neispravanIliNeuskladjenIndeksSeOdbija(String indeks) {
        StudentRequest request = studentRequest();
        request.setBrIndeksa(indeks);
        assertThrows(BadRequestException.class, () -> service.dodajStudenta(request));
        verifyNoInteractions(korisnici);
        verify(studenti, never()).saveAndFlush(any());
    }

    @Test
    void imeOdKogNijeMoguceNapravitiEmailSeOdbija() {
        ProfesorRequest request = profesorRequest();
        request.setIme("123");
        assertThrows(BadRequestException.class, () -> service.dodajProfesora(request));
        verify(profesori, never()).saveAndFlush(any());
        verify(korisnici, never()).saveAndFlush(any());
    }

    @Test
    void konkurentniDuplikatNaUpisuNalogaVraca409() {
        when(profesori.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(korisnici.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplikat"));
        assertThrows(ConflictException.class, () -> service.dodajProfesora(profesorRequest()));
    }

    private Korisnik sacuvaniKorisnik() {
        ArgumentCaptor<Korisnik> captor = ArgumentCaptor.forClass(Korisnik.class);
        verify(korisnici).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static StudentRequest studentRequest() {
        StudentRequest request = new StudentRequest();
        request.setIme("Marko");
        request.setPrezime("Nikolić");
        request.setSmer("rn");
        request.setBrIndeksa("12/2023");
        request.setGodUpisa(2023);
        return request;
    }

    private static ProfesorRequest profesorRequest() {
        ProfesorRequest request = new ProfesorRequest();
        request.setIme("Marina");
        request.setPrezime("Petrović");
        return request;
    }
}
