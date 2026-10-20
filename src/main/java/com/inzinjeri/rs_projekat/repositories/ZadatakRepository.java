package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Zadatak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZadatakRepository extends JpaRepository<Zadatak, Long> {
}
