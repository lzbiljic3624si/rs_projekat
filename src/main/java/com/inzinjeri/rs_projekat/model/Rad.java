package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name="Rad")
@ToString
public class Rad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long PrijavaID;
    @Column(nullable = false)
    private Long ZadatakID;
    private LocalDateTime VremePreuzimanja;
    private LocalDateTime VremePredaje;
    private String RepoPutanjaResenja;
    private Double Poeni;
    @Column(nullable = false)
    private String Status;
}
