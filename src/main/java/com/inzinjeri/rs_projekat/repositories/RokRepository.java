package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Rok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RokRepository extends JpaRepository<Rok, Long> {

    boolean existsByNazivAndSkolskaGodina(String naziv, Integer skolskaGodina);

    @Query("SELECT r FROM Rok r WHERE r.naziv = :naziv")
    Optional<Rok> pronadjiPoNazivu(@Param("naziv") String naziv);
}
