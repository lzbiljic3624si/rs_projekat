package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Zadatak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZadatakRepository extends JpaRepository<Zadatak, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from Zadatak z where z.id = :id")
    Optional<Zadatak> findByIdZaIzmenu(@Param("id") Long id);

    boolean existsByRepoPutanja(String repoPutanja);

    @Query("SELECT z FROM Zadatak z WHERE z.naziv = :naziv")
    Optional<Zadatak> pronadjiPoNazivu(@Param("naziv") String naziv);


    @Query("SELECT z FROM Zadatak z WHERE z.grupa.id = :grupaId")
    Optional<Zadatak> pronadjiPoGrupi(@Param("grupaId") Long grupaId);

    boolean existsByGrupa_Id(Long grupaId);
}
