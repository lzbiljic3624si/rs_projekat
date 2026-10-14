package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name="Termin")
@ToString
public class Termin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate Datum;
    @Column(nullable = false)
    private LocalTime VremePocetak;
    @Column(nullable = false)
    private LocalTime VremeKraj;
    @Column(nullable = false)
    private Long UcionicaID;
    @Column(nullable = false)
    private Long PredmetID;
    @Column(nullable = false)
    private Long RokID;
}
