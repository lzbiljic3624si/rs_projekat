package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.PrijavljenStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrijavljenStudentRepository extends JpaRepository<PrijavljenStudent, Long> {
    @Query("select ps from PrijavljenStudent ps where ps.student.email = :emailStudenta")
    List<PrijavljenStudent> getStudentByEmail (
            @Param("emailStudenta") String emailStudenta
    );

    ///DODATI PROVERU PRETRAGU PO GRUPI I TERMINU
}
