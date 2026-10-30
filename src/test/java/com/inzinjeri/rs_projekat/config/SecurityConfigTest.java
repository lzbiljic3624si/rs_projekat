package com.inzinjeri.rs_projekat.config;

import com.inzinjeri.rs_projekat.model.Profesor;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.repositories.ProfesorRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void loadsStudentAndProfesorButRejectsAmbiguousEmail() {
        var students = mock(StudentRepository.class);
        var profesori = mock(ProfesorRepository.class);
        var student = new Student();
        student.setPasswordHash("student-hash");
        var profesor = new Profesor();
        profesor.setPasswordHash("profesor-hash");
        var config = new SecurityConfig();

        when(students.pronadjiPoEmailu("student@example.com")).thenReturn(Optional.of(student));
        when(profesori.getProfesorByEmail("student@example.com")).thenReturn(List.of());
        when(students.pronadjiPoEmailu("profesor@example.com")).thenReturn(Optional.empty());
        when(profesori.getProfesorByEmail("profesor@example.com")).thenReturn(List.of(profesor));
        when(students.pronadjiPoEmailu("duplikat@example.com")).thenReturn(Optional.of(student));
        when(profesori.getProfesorByEmail("duplikat@example.com")).thenReturn(List.of(profesor));

        var users = config.userDetailsService(students, profesori);
        assertEquals("student-hash", users.loadUserByUsername("student@example.com").getPassword());
        assertEquals("ROLE_STUDENT", users.loadUserByUsername("student@example.com").getAuthorities().iterator().next().getAuthority());
        assertEquals("profesor-hash", users.loadUserByUsername("profesor@example.com").getPassword());
        assertEquals("ROLE_PROFESOR", users.loadUserByUsername("profesor@example.com").getAuthorities().iterator().next().getAuthority());
        assertThrows(UsernameNotFoundException.class, () -> users.loadUserByUsername("duplikat@example.com"));
        assertThrows(UsernameNotFoundException.class, () -> users.loadUserByUsername("nepoznat@example.com"));

        when(profesori.getProfesorByEmail("vise@example.com")).thenReturn(List.of(profesor, profesor));
        assertThrows(UsernameNotFoundException.class, () -> users.loadUserByUsername("vise@example.com"));
    }
}
