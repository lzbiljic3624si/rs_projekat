package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Grupa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrupaRepository extends JpaRepository<Grupa, Long> {

    boolean existsByNaziv(String naziv);

    @Query("select g from Grupa g where g.termin.id = :idTermina")
    List<Grupa> getGrupeZaTermin(@Param("idTermina") Long idTermina);
}
