package com.inzinjeri.rs_projekat.config;

import com.inzinjeri.rs_projekat.controllers.PredmetController;
import com.inzinjeri.rs_projekat.model.Predmet;
import com.inzinjeri.rs_projekat.model.Student;
import com.inzinjeri.rs_projekat.repositories.ProfesorRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import com.inzinjeri.rs_projekat.services.PredmetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PredmetController.class)
@Import(SecurityConfig.class)
class SecurityHttpTest {
    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private PredmetService predmeti;
    @MockitoBean private StudentRepository studenti;
    @MockitoBean private ProfesorRepository profesori;

    @Test
    void neprijavljenKorisnikSePreusmeravaNaLogin() throws Exception {
        mvc.perform(get("/api/predmet/all")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(predmeti);
    }

    @Test
    void prijavljenKorisnikMozeDaCita() throws Exception {
        when(predmeti.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/predmet/all").with(user("student@test.rs").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(predmeti).findAll();
    }

    @Test
    void postBezCsrfTokenaSeOdbija() throws Exception {
        mvc.perform(post("/api/predmet/add").with(user("profesor@test.rs").roles("PROFESOR")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(predmeti);
    }

    @Test
    void csrfTokenPropustaZahtevDoValidacije() throws Exception {
        mvc.perform(post("/api/predmet/add").with(user("profesor@test.rs").roles("PROFESOR")).with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(predmeti);
    }

    @Test
    void studentNeMozeDaDodaPredmetNiSaCsrfTokenom() throws Exception {
        Predmet predmet = new Predmet();
        predmet.setId(1L);
        when(predmeti.save(any())).thenReturn(predmet);

        mvc.perform(post("/api/predmet/add").with(user("student@test.rs").roles("STUDENT")).with(csrf())
                        .contentType("application/json").content("""
                                {"naziv":"Razvoj softvera","jezik":"Java","uslov":"OOP"}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(predmeti);
    }

    @Test
    void profesorMozeDaDodaPredmet() throws Exception {
        Predmet predmet = new Predmet();
        predmet.setId(1L);
        when(predmeti.save(any())).thenReturn(predmet);

        mvc.perform(post("/api/predmet/add").with(user("profesor@test.rs").roles("PROFESOR")).with(csrf())
                        .contentType("application/json").content("""
                                {"naziv":"Razvoj softvera","jezik":"Java","uslov":"OOP"}
                                """))
                .andExpect(status().isCreated()).andExpect(content().string("1"));
        verify(predmeti).save(argThat(p -> "Razvoj softvera".equals(p.getNaziv())
                && "Java".equals(p.getJezik()) && "OOP".equals(p.getUslov())));
    }

    @Test
    void loginKoristiEmailIBcryptLozinku() throws Exception {
        Student student = new Student();
        student.setPasswordHash(encoder.encode("test-lozinka"));
        when(studenti.pronadjiPoEmailu("student@test.rs")).thenReturn(Optional.of(student));
        when(profesori.getProfesorByEmail("student@test.rs")).thenReturn(List.of());

        mvc.perform(formLogin("/api/auth/login").user("email", "student@test.rs").password("test-lozinka"))
                .andExpect(authenticated().withUsername("student@test.rs").withRoles("STUDENT"));
        mvc.perform(formLogin("/api/auth/login").user("email", "student@test.rs").password("pogresna"))
                .andExpect(unauthenticated());
    }
}
