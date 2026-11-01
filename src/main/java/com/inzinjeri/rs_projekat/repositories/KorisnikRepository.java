package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Korisnik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KorisnikRepository extends JpaRepository<Korisnik, Long> {

    Optional<Korisnik> findByEmail(String email);

    Optional<Korisnik> findByStudent_Id(Long studentId);

    Optional<Korisnik> findByProfesor_Id(Long profesorId);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
}
