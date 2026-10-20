package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Zadatak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZadatakRepository extends JpaRepository<Zadatak, Long> {

    @Query("SELECT z FROM Zadatak z WHERE z.naziv = :naziv")
    Optional<Zadatak> pronadjiPoNazivu(@Param("naziv") String naziv);


    @Query("SELECT z FROM Zadatak z WHERE z.grupa.id = :grupaId")
    List<Zadatak> pronadjiPoGrupi(@Param("grupaId") Long grupaId);
}
