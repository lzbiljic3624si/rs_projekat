package com.inzinjeri.rs_projekat.config;

import com.inzinjeri.rs_projekat.repositories.ProfesorRepository;
import com.inzinjeri.rs_projekat.repositories.StudentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {  // SecurityFilterChain — gde se korisnik prijavljuje
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .permitAll());
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(StudentRepository students, ProfesorRepository profesori) { // UserDetailsService — koga prijavljujemo
        return email -> {
            var student = students.pronadjiPoEmailu(email);
            var profesoriSaEmailom = profesori.getProfesorByEmail(email);

            if (student.isPresent() && profesoriSaEmailom.isEmpty()) {
                return User.withUsername(email)
                    .password(student.get().getPasswordHash())
                    .roles("STUDENT")
                    .build();
            }
            if (student.isEmpty() && profesoriSaEmailom.size() == 1) {
                return User.withUsername(email)
                    .password(profesoriSaEmailom.getFirst().getPasswordHash())
                    .roles("PROFESOR")
                    .build();
            }
            throw new UsernameNotFoundException("Nalog ne postoji ili email nije jedinstven");
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    } // PasswordEncoder — da li je lozinka tačna
}
