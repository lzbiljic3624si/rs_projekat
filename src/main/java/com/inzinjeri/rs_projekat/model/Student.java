package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String smer;
    @Column(nullable = false)
    private String ime;
    @Column(nullable = false)
    private String prezime;
    @Column(nullable = false, unique = true)
    private String brIndeksa;
    @Column(nullable = false)
    private Integer godUpisa;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
}
