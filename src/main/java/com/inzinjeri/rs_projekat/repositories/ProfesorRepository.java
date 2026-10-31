package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<Profesor> findByEmailIgnoreCase(String email);

    @Query("select p from Profesor p where p.email = :emailProfesora")
    List<Profesor> getProfesorByEmail(
            @Param("emailProfesora") String emailProfesora
    );
}
