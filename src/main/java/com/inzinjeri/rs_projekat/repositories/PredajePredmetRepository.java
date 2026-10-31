package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.PredajePredmet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PredajePredmetRepository extends JpaRepository<PredajePredmet, Long> {
    boolean existsByProfesor_IdAndPredmet_Id(Long profesorId, Long predmetId);

    @Query("select pp from PredajePredmet pp where pp.profesor.email = :emailPredavaca")
    List<PredajePredmet> getPredmetiZaPredavaca(
            @Param("emailPredavaca") String emailPredavaca
    );

    @Query("select pp from PredajePredmet pp where pp.predmet.naziv = :nazivPredmeta")
    List<PredajePredmet> getPredavaciPredmeta(
            @Param("nazivPredmeta") String nazivPredmeta
    );
}
