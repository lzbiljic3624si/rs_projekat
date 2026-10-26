package com.inzinjeri.rs_projekat.controllers.response;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TerminResponse {
    private Long id;
    private LocalDate datum;
    private LocalTime vremePocetka;
    private LocalTime vremeKraja;
    private UcionicaResponse ucionica;
    private PredmetResponse predmet;
    private RokResponse rok;
}
