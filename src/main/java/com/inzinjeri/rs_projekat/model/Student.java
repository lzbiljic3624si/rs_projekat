package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="Student")
@ToString
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String Smer;
    @Column(nullable = false)
    private String Ime;
    @Column(nullable = false)
    private String Prezime;
    @Column(nullable = false, unique = true)
    private String BrIndeksa;
    @Column(nullable = false)
    private Integer GodUpisa;
    @Column(nullable = false, unique = true)
    private String Email;
    @Column(nullable = false)
    private String PasswordHash;
}
