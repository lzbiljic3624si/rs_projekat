package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="Komentar")
@ToString
public class Komentar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long RadID;
    @Column(nullable = false)
    private Long ProfesorID;
    @Column(nullable = false)
    private String Fajl;
    @Column(nullable = false)
    private Integer Linija;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String Tekst;
}
