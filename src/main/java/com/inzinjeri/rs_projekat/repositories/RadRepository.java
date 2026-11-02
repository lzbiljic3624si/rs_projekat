package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.StatusRada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RadRepository extends JpaRepository<Rad, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rad r where r.id = :id")
    Optional<Rad> findByIdZaIzmenu(@Param("id") Long id);

    boolean existsByStudent_Id(Long studentId);
    @Query("select r from Rad r where r.student.email = :emailStudenta")
    List<Rad> getRadByStudentEmail(
            @Param("emailStudenta") String emailStudenta
    );

    // Da li ovaj student već ima rad za ovaj zadatak?
    boolean existsByStudent_IdAndZadatak_Id(Long studentId, Long zadatakId);

    boolean existsByZadatak_Id(Long zadatakId);

    boolean existsByStudent_IdAndZadatak_Grupa_Id(Long studentId, Long grupaId);

    boolean existsByZadatak_Grupa_Id(Long grupaId);

    List<Rad> findByZadatak_Grupa_Termin_Id(Long terminId);

    @Query("select r from Rad r where r.zadatak.grupa.termin.predmet.id = :predmetId and (:status is null or r.status = :status)")
    List<Rad> findByPredmetIdAndStatus(@Param("predmetId") Long predmetId, @Param("status") StatusRada status);
}
