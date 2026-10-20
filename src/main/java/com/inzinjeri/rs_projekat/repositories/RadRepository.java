package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Rad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RadRepository extends JpaRepository<Rad, Long> {
    @Query("select r from Rad r where r.student.email = :emailStudenta")
    List<Rad> getRadByStudentEmail(
            @Param("emailStudenta") String emailStudenta
    );
}
