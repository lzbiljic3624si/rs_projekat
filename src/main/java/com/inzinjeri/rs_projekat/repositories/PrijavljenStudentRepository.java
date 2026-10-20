package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.PrijavljenStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrijavljenStudentRepository extends JpaRepository<PrijavljenStudent, Long> {
}
