package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.exceptions.ConflictException;
import com.inzinjeri.rs_projekat.model.Termin;
import java.time.LocalDateTime;

public final class VremeTermina {
    private VremeTermina() {}

    public static LocalDateTime pocetak(Termin termin) {
        if (termin == null || termin.getDatum() == null || termin.getVremePocetak() == null
                || termin.getVremeKraj() == null || !termin.getVremePocetak().isBefore(termin.getVremeKraj())) {
            throw new ConflictException("Termin nema ispravan datum i vreme");
        }
        return termin.getDatum().atTime(termin.getVremePocetak());
    }

    public static void proveriPrePocetka(Termin termin, LocalDateTime sada) {
        if (!sada.isBefore(pocetak(termin))) {
            throw new ConflictException("Termin je vec poceo");
        }
    }

    public static void proveriUToku(Termin termin, LocalDateTime sada) {
        LocalDateTime pocetak = pocetak(termin);
        if (sada.isBefore(pocetak) || !sada.isBefore(termin.getDatum().atTime(termin.getVremeKraj()))) {
            throw new ConflictException("Rad je dozvoljen samo tokom termina");
        }
    }
}
