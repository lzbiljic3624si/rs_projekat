package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="Zadatak")
@ToString
public class Zadatak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String Naziv;
    @Column(nullable = false)
    private Long GrupaID;
    @Column(nullable = false)
    private String RepoPutanja;
}
