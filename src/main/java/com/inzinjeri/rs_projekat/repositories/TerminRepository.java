package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Termin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TerminRepository extends JpaRepository<Termin, Long> {

    @Query("SELECT t FROM Termin t WHERE t.datum = :datum")
    List<Termin> pronadjiPoDatumu(@Param("datum") LocalDate datum);

    @Query("SELECT t FROM Termin t WHERE t.predmet.id = :predmetId")
    List<Termin> pronadjiPoPredmetu(@Param("predmetId") Long predmetId);

    @Query("SELECT t FROM Termin t WHERE t.rok.id = :rokId")
    List<Termin> pronadjiPoRoku(@Param("rokId") Long rokId);

    @Query("SELECT t FROM Termin t WHERE t.ucionica.id = :ucionicaId")
    List<Termin> pronadjiPoUcionici(@Param("ucionicaId") Long ucionicaId);

    @Query("SELECT t FROM Termin t WHERE t.predmet.id = :predmetId AND t.rok.id = :rokId")
    List<Termin> pronadjiPoPredmetuIRoku(
            @Param("predmetId") Long predmetId,
            @Param("rokId") Long rokId
    );
}
