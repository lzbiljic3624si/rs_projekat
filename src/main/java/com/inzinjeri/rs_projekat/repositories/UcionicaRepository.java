package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Ucionica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UcionicaRepository extends JpaRepository<Ucionica, Long> {

    @Query("SELECT u FROM Ucionica u WHERE u.naziv = :naziv")
    Optional<Ucionica> pronadjiPoNazivu(@Param("naziv") String naziv);
}
