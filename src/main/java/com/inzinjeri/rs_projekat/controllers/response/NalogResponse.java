package com.inzinjeri.rs_projekat.controllers.response;

import com.inzinjeri.rs_projekat.model.Korisnik;
import com.inzinjeri.rs_projekat.model.Uloga;

public record NalogResponse(Long id, String email, Uloga uloga, Long studentId, Long profesorId) {
    public static NalogResponse from(Korisnik nalog) {
        return new NalogResponse(nalog.getId(), nalog.getEmail(), nalog.getUloga(),
                nalog.getStudent() == null ? null : nalog.getStudent().getId(),
                nalog.getProfesor() == null ? null : nalog.getProfesor().getId());
    }
}
