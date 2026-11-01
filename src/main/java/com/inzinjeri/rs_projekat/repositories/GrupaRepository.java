package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Grupa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GrupaRepository extends JpaRepository<Grupa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Grupa g where g.id = :id")
    Optional<Grupa> findByIdZaIzmenu(@Param("id") Long id);

    boolean existsByNaziv(String naziv);

    boolean existsByTermin_Id(Long terminId);

    @Query("select g from Grupa g where g.termin.id = :idTermina")
    List<Grupa> getGrupeZaTermin(@Param("idTermina") Long idTermina);
}
