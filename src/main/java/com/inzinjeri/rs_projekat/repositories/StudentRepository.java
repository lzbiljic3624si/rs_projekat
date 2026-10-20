package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE s.brIndeksa = :brIndeksa")
    Optional<Student> pronadjiPoBrojuIndeksa(@Param("brIndeksa") String brIndeksa);

    @Query("SELECT s FROM Student s WHERE s.email = :email")
    Optional<Student> pronadjiPoEmailu(@Param("email") String email);
}
