package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@Table(name="termin")
public class Termin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate datum;
    @Column(nullable = false)
    private LocalTime vremePocetak;
    @Column(nullable = false)
    private LocalTime vremeKraj;

    @ManyToOne
    @JoinColumn(name = "ucionica_id")
    private Ucionica ucionica;

    @ManyToOne
    @JoinColumn(name = "predmet_id")
    private Predmet predmet;

    @ManyToOne
    @JoinColumn(name = "rok_id")
    private Rok rok;
}
