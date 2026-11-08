package com.inzinjeri.rs_projekat.repositories;

import com.inzinjeri.rs_projekat.model.Rok;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RokRepository extends JpaRepository<Rok, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rok r where r.id = :id")
    Optional<Rok> findByIdZaIzmenu(@Param("id") Long id);

    // Vrednost i zakljucavanje iz istog citanja, i pod MySQL REPEATABLE READ izolacijom.
    @Query(value = "select r.repo_putanja from rok r join termin t on t.rok_id = r.id "
            + "join grupa g on g.termin_id = t.id where g.id = :grupaId for update", nativeQuery = true)
    Optional<String> repoZaGrupuZaIzmenu(@Param("grupaId") Long grupaId);

    boolean existsByNazivAndSkolskaGodina(String naziv, Integer skolskaGodina);
    boolean existsByRepoPutanjaIgnoreCaseAndIdNot(String repoPutanja, Long id);

    @Query("SELECT r FROM Rok r WHERE r.naziv = :naziv")
    Optional<Rok> pronadjiPoNazivu(@Param("naziv") String naziv);
}
