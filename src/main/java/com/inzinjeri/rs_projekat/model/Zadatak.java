package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="zadatak")
public class Zadatak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    @ManyToOne
    @JoinColumn(name = "grupa_id")
    private Grupa grupa;

    @Column(nullable = false, unique = true)
    private String repoPutanja;
}
