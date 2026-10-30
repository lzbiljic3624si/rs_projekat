package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "korisnik")
public class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Uloga uloga;

    @OneToOne
    @JoinColumn(name = "student_id", unique = true)
    private Student student;

    @OneToOne
    @JoinColumn(name = "profesor_id", unique = true)
    private Profesor profesor;
}
