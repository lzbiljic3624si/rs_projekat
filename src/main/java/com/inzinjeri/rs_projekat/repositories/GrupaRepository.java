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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Grupa g where g.naziv = :naziv "
            + "and g.termin.predmet.id = :predmetId and g.termin.rok.id = :rokId "
            + "and (:izuzetiId is null or g.id <> :izuzetiId)")
    List<Grupa> pronadjiIstoimeneZaPredmetIRok(@Param("naziv") String naziv,
                                             @Param("predmetId") Long predmetId,
                                             @Param("rokId") Long rokId,
                                             @Param("izuzetiId") Long izuzetiId);

    boolean existsByTermin_Id(Long terminId);

    @Query("select g from Grupa g where g.termin.id = :idTermina")
    List<Grupa> getGrupeZaTermin(@Param("idTermina") Long idTermina);
}
