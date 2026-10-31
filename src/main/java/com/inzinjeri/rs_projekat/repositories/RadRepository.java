package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Rad;
import com.inzinjeri.rs_projekat.model.StatusRada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RadRepository extends JpaRepository<Rad, Long> {
    @Query("select r from Rad r where r.student.email = :emailStudenta")
    List<Rad> getRadByStudentEmail(
            @Param("emailStudenta") String emailStudenta
    );

    // Da li ovaj student već ima rad za ovaj zadatak?
    boolean existsByStudent_IdAndZadatak_Id(Long studentId, Long zadatakId);

    @Query("select r from Rad r where r.zadatak.grupa.termin.predmet.id = :predmetId and r.status = :status")
    List<Rad> findByPredmetIdAndStatus(@Param("predmetId") Long predmetId, @Param("status") StatusRada status);
}
