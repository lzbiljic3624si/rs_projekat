package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Komentar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KomentarRepository extends JpaRepository<Komentar,Long> {
    List<Komentar> findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(Long radId);
}
