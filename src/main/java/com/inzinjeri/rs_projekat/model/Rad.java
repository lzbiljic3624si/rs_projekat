package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="rad")
public class Rad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "zadatak_id")
    private Zadatak zadatak;

    private LocalDateTime vremePreuzimanja;
    private LocalDateTime vremePredaje;
    private String repoPutanjaResenja;
    private Double poeni;

    @Column(nullable = false)
    private String status;
}
