package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.model.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityMappersTest {
    @Test
    void radMapiraUgnjezdeneVezeVremenaIPoljaBezLozinke() {
        Student student = new Student(); student.setId(2L); student.setIme("Ana"); student.setPrezime("Anic");
        student.setBrIndeksa("RN2312"); student.setGodUpisa(2023); student.setEmail("student@test.rs");
        student.setPasswordHash("tajni-hash");
        Predmet predmet = new Predmet(); predmet.setId(4L); predmet.setNaziv("Razvoj softvera");
        predmet.setJezik("Java"); predmet.setUslov("Nema");
        Rok rok = new Rok(); rok.setId(6L); rok.setNaziv("Junski"); rok.setSkolskaGodina(2026);
        Ucionica ucionica = new Ucionica(); ucionica.setId(5L); ucionica.setNaziv("Lab 1");
        Termin termin = new Termin(); termin.setId(7L); termin.setPredmet(predmet); termin.setRok(rok);
        termin.setUcionica(ucionica); termin.setDatum(LocalDate.of(2026, 10, 30));
        termin.setVremePocetak(LocalTime.of(10, 0)); termin.setVremeKraj(LocalTime.of(12, 0));
        Grupa grupa = new Grupa(); grupa.setId(3L); grupa.setNaziv("G1"); grupa.setTermin(termin);
        Zadatak zadatak = new Zadatak(); zadatak.setId(9L); zadatak.setNaziv("Prvi zadatak");
        zadatak.setGrupa(grupa); zadatak.setRepoPutanja("https://github.com/test/zadatak");
        Rad rad = new Rad(); rad.setId(10L); rad.setStudent(student); rad.setZadatak(zadatak);
        rad.setStatus("OCENJEN"); rad.setPoeni(8.5); rad.setRepoPutanjaResenja("https://github.com/test/resenje");
        rad.setVremePreuzimanja(LocalDateTime.of(2026, 10, 30, 10, 0));
        rad.setVremePredaje(LocalDateTime.of(2026, 10, 30, 11, 0));

        var response = EntityMappers.toResponse(rad);
        assertEquals(rad.getId(), response.getId());
        assertEquals("Ana", response.getStudent().getIme());
        assertEquals("Anic", response.getStudent().getPrezime());
        assertEquals("RN2312", response.getStudent().getBrIndeksa());
        assertEquals(2023, response.getStudent().getGodUpisa());
        assertEquals(student.getEmail(), response.getStudent().getEmail());
        assertEquals(rad.getVremePreuzimanja(), response.getVremePreuzimanja());
        assertEquals(rad.getVremePredaje(), response.getVremePredaje());
        assertEquals(rad.getRepoPutanjaResenja(), response.getRepoPutanjaResenja());
        assertEquals(8.5, response.getPoeni());
        assertEquals("OCENJEN", response.getStatus());
        assertEquals("Prvi zadatak", response.getZadatak().getNaziv());
        assertEquals(zadatak.getRepoPutanja(), response.getZadatak().getRepoPutanja());
        assertEquals("G1", response.getZadatak().getGrupa().getNaziv());
        var terminResponse = response.getZadatak().getGrupa().getTermin();
        assertEquals(termin.getDatum(), terminResponse.getDatum());
        assertEquals(termin.getVremePocetak(), terminResponse.getVremePocetka());
        assertEquals(termin.getVremeKraj(), terminResponse.getVremeKraja());
        assertEquals("Razvoj softvera", terminResponse.getPredmet().getNaziv());
        assertEquals("Java", terminResponse.getPredmet().getJezik());
        assertEquals("Nema", terminResponse.getPredmet().getUslov());
        assertEquals("Junski", terminResponse.getRok().getNaziv());
        assertEquals(2026, terminResponse.getRok().getSkolskaGodina());
        assertEquals("Lab 1", terminResponse.getUcionica().getNaziv());
        String json = JsonMapper.builder().build().writeValueAsString(response);
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("tajni-hash"));
    }

    @Test
    void profesorIPrijavaMapirajuSvojeVeze() {
        Profesor profesor = new Profesor(); profesor.setId(1L); profesor.setIme("Petar");
        profesor.setPrezime("Petrovic"); profesor.setEmail("profesor@test.rs"); profesor.setPasswordHash("tajni-hash");
        Predmet predmet = new Predmet(); predmet.setId(4L);
        PredajePredmet veza = new PredajePredmet(); veza.setId(2L); veza.setProfesor(profesor); veza.setPredmet(predmet);
        var response = EntityMappers.toResponse(veza);
        assertEquals(2L, response.getId());
        assertEquals(4L, response.getPredmet().getId());
        assertEquals("Petar", response.getProfesor().getIme());
        assertEquals("Petrovic", response.getProfesor().getPrezime());
        assertEquals(profesor.getEmail(), response.getProfesor().getEmail());
        assertFalse(JsonMapper.builder().build().writeValueAsString(response).contains("tajni-hash"));
        PrijavljenStudent prijava = new PrijavljenStudent(); prijava.setId(8L);
        Student student = new Student(); student.setId(3L); prijava.setStudent(student);
        Grupa grupa = new Grupa(); grupa.setId(5L); prijava.setGrupa(grupa);
        var prijavaResponse = EntityMappers.toResponse(prijava);
        assertEquals(8L, prijavaResponse.getId());
        assertFalse(JsonMapper.builder().build().writeValueAsString(prijavaResponse).contains("\"status\""));
        assertEquals(3L, prijavaResponse.getStudent().getId());
        assertEquals(5L, prijavaResponse.getGrupa().getId());
    }

    @Test
    void nullEntitetiIVezeSuDozvoljeni() {
        assertNull(EntityMappers.toResponse((Rad) null));
        assertNull(EntityMappers.toResponse((Zadatak) null));
        assertNull(EntityMappers.toResponse((Student) null));
        assertNull(EntityMappers.toResponse((Profesor) null));
        assertNull(EntityMappers.toResponse((Predmet) null));
        assertNull(EntityMappers.toResponse((Termin) null));
        assertNull(EntityMappers.toResponse((Grupa) null));
        assertNull(EntityMappers.toResponse((Rok) null));
        assertNull(EntityMappers.toResponse((Ucionica) null));
        assertNull(EntityMappers.toResponse((PrijavljenStudent) null));
        assertNull(EntityMappers.toResponse((PredajePredmet) null));
        var rad = EntityMappers.toResponse(new Rad());
        assertNull(rad.getStudent());
        assertNull(rad.getZadatak());
    }
}
