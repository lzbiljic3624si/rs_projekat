package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
