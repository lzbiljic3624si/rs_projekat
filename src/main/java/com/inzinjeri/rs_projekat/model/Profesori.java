package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="Profesori")
@ToString

public class Profesori {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String Ime;

    @Column(nullable = false)
    private String Prezime;

    @Column(nullable = false, unique = true)
    private String Email;

    @Column(nullable = false)
    private String PasswordHash;
}