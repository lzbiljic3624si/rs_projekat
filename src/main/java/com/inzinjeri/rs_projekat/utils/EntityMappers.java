package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.controllers.response.*;
import com.inzinjeri.rs_projekat.model.*;

public final class EntityMappers {

    private EntityMappers() {
    }

    public static RokResponse toResponse(Rok rok) {
        if (rok == null) return null;
        RokResponse response = new RokResponse();
        response.setId(rok.getId());
        response.setNaziv(rok.getNaziv());
        response.setSkolskaGodina(rok.getSkolskaGodina());
        return response;
    }

    public static UcionicaResponse toResponse(Ucionica ucionica) {
        if (ucionica == null) return null;
        UcionicaResponse response = new UcionicaResponse();
        response.setId(ucionica.getId());
        response.setNaziv(ucionica.getNaziv());
        return response;
    }

    public static PredmetResponse toResponse(Predmet predmet) {
        if (predmet == null) return null;
        PredmetResponse response = new PredmetResponse();
        response.setId(predmet.getId());
        response.setNaziv(predmet.getNaziv());
        response.setJezik(predmet.getJezik());
        response.setUslov(predmet.getUslov());
        return response;
    }

    public static TerminResponse toResponse(Termin termin) {
        if (termin == null) return null;
        TerminResponse response = new TerminResponse();
        response.setId(termin.getId());
        response.setDatum(termin.getDatum());
        response.setVremePocetka(termin.getVremePocetak());
        response.setVremeKraja(termin.getVremeKraj());
        response.setUcionica(toResponse(termin.getUcionica()));
        response.setPredmet(toResponse(termin.getPredmet()));
        response.setRok(toResponse(termin.getRok()));
        return response;
    }

    public static GrupaResponse toResponse(Grupa grupa) {
        if (grupa == null) return null;
        GrupaResponse response = new GrupaResponse();
        response.setId(grupa.getId());
        response.setNaziv(grupa.getNaziv());
        response.setTermin(toResponse(grupa.getTermin()));
        return response;
    }

    public static StudentResponse toResponse(Student student) {
        if (student == null) return null;
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setIme(student.getIme());
        response.setPrezime(student.getPrezime());
        response.setBrIndeksa(student.getBrIndeksa());
        response.setGodUpisa(student.getGodUpisa());
        response.setEmail(student.getEmail());
        return response;
    }

    public static PrijavljenStudentResponse toResponse(PrijavljenStudent prijava) {
        PrijavljenStudentResponse response = new PrijavljenStudentResponse();
        response.setId(prijava.getId());
        response.setStudent(toResponse(prijava.getStudent()));
        response.setGrupa(toResponse(prijava.getGrupa()));
        response.setStatus(prijava.getStatus());
        return response;
    }

    public static ZadatakResponse toResponse(Zadatak zadatak) {
        ZadatakResponse response = new ZadatakResponse();
        response.setId(zadatak.getId());
        response.setNaziv(zadatak.getNaziv());
        response.setGrupa(toResponse(zadatak.getGrupa()));
        response.setRepoPutanja(zadatak.getRepoPutanja());
        return response;
    }
}
