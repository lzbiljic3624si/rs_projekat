package com.inzinjeri.rs_projekat;

import com.inzinjeri.rs_projekat.repositories.KomentarRepository;
import com.inzinjeri.rs_projekat.repositories.RadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:seeder_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class SeederTest {

    @Autowired Seeder seeder;
    @Autowired KomentarRepository komentari;
    @Autowired RadRepository radovi;

    @Test
    void popunjavaPrazneKomentareKadaRadoviVecPostoje() throws Exception {
        var radIds = radovi.findAll().stream().map(r -> r.getId()).sorted().toList();
        komentari.deleteAll();
        komentari.flush();
        assertEquals(0, komentari.count());

        seeder.run();

        assertEquals(5, komentari.count());
        assertEquals(radIds, radovi.findAll().stream().map(r -> r.getId()).sorted().toList());
        for (var radId : radIds) {
            assertEquals(1, komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(radId).size());
        }
        var komentarIds = komentari.findAll().stream().map(k -> k.getId()).sorted().toList();
        seeder.run();
        assertEquals(komentarIds, komentari.findAll().stream().map(k -> k.getId()).sorted().toList());
    }

    @Test
    void pocetniRadoviImajuValidneKomentareBezDupliranjaPriPonovnomPokretanju() throws Exception {
        assertEquals(5, komentari.count());
        for (var rad : radovi.findAll()) {
            var lista = komentari.findByRad_IdOrderByPutanjaFajlaAscBrojLinijeAsc(rad.getId());
            assertEquals(1, lista.size());
            var komentar = lista.getFirst();
            assertFalse(komentar.getKomentar().isBlank());
            assertTrue(komentar.getKomentar().length() <= 2000);
            assertFalse(komentar.getPutanjaFajla().isBlank());
            assertTrue(komentar.getPutanjaFajla().length() <= 500);
            assertTrue(komentar.getBrojLinije() > 0);
        }

        var ids = komentari.findAll().stream().map(k -> k.getId()).toList();
        seeder.run();
        assertEquals(ids, komentari.findAll().stream().map(k -> k.getId()).toList());
    }
}
