package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TerminRequest {
    @NotNull
    @FutureOrPresent //Ogranicava da datum ne moze biti u proslosti
    private LocalDate datum;
    @NotNull
    private LocalTime vremePocetak;
    @NotNull
    private LocalTime vremeKraja;
    @NotNull
    private Long ucionicaId;
    @NotNull
    private Long predmetId;
    @NotNull
    private Long rokId;
}
