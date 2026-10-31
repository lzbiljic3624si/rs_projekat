package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.controllers.response.*;
import com.inzinjeri.rs_projekat.model.*;
import com.inzinjeri.rs_projekat.services.AuthService;

public final class EntityMappers {

    private EntityMappers() {
    }

    public static LoginResponse toLoginResponse(AuthService.Prijava prijava) {
        return new LoginResponse(prijava.token(), prijava.istice(), prijava.uloga());
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
        response.setSmer(student.getSmer());
        response.setBrIndeksa(student.getBrIndeksa());
        response.setGodUpisa(student.getGodUpisa());
        response.setEmail(student.getEmail());
        return response;
    }

    public static PrijavljenStudentResponse toResponse(PrijavljenStudent prijava) {
        if (prijava == null) return null;
        PrijavljenStudentResponse response = new PrijavljenStudentResponse();
        response.setId(prijava.getId());
        response.setStudent(toResponse(prijava.getStudent()));
        response.setGrupa(toResponse(prijava.getGrupa()));
        return response;
    }

    public static ZadatakResponse toResponse(Zadatak zadatak) {
        if (zadatak == null) return null;
        ZadatakResponse response = new ZadatakResponse();
        response.setId(zadatak.getId());
        response.setNaziv(zadatak.getNaziv());
        response.setGrupa(toResponse(zadatak.getGrupa()));
        response.setRepoPutanja(zadatak.getRepoPutanja());
        return response;
    }

    public static RadResponse toResponse(Rad rad) {
        if (rad == null) return null;
        RadResponse response = new RadResponse();
        response.setId(rad.getId());
        response.setStudent(toResponse(rad.getStudent()));
        response.setZadatak(toResponse(rad.getZadatak()));
        response.setRepoPutanjaResenja(rad.getRepoPutanjaResenja());
        response.setPoeni(rad.getPoeni());
        response.setStatus(rad.getStatus());
        response.setVremePreuzimanja(rad.getVremePreuzimanja());
        response.setVremePredaje(rad.getVremePredaje());
        return response;
    }

    public static ProfesorResponse toResponse(Profesor profesor) {
        if (profesor == null) return null;
        ProfesorResponse response = new ProfesorResponse();
        response.setId(profesor.getId());
        response.setIme(profesor.getIme());
        response.setPrezime(profesor.getPrezime());
        response.setEmail(profesor.getEmail());
        return response;
    }

    public static PredajePredmetResponse toResponse(PredajePredmet predajePredmet) {
        if (predajePredmet == null) return null;
        PredajePredmetResponse response = new PredajePredmetResponse();
        response.setId(predajePredmet.getId());
        response.setProfesor(toResponse(predajePredmet.getProfesor()));
        response.setPredmet(toResponse(predajePredmet.getPredmet()));
        return response;
    }
}
