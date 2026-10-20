package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Predmet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PredmetRepository extends JpaRepository<Predmet, Long> {
    @Query("select p from Predmet p where p.naziv = :nazivPredmeta")
    List<Predmet> getPredmetByNaziv(
            @Param("nazivPredmeta") String nazivPredmeta
    );

    @Query("select p from Predmet p where p.jezik = :jezikPredmeta")
    List<Predmet> getPredmetByJezik (
            @Param("jezikPredmeta") String jezikPredmeta
    );
}
