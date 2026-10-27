package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Termin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TerminRepository extends JpaRepository<Termin, Long> {

    @Query("select count(t) > 0 from Termin t where t.ucionica.id = :ucionicaId and t.datum = :datum and t.vremePocetak < :kraj and t.vremeKraj > :pocetak")
    boolean postojiPreklapanje(@Param("ucionicaId") Long ucionicaId, @Param("datum") LocalDate datum,
                               @Param("pocetak") LocalTime pocetak, @Param("kraj") LocalTime kraj);

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
