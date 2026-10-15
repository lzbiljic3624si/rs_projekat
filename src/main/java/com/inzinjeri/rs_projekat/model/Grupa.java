package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="grupa")
public class Grupa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    @ManyToOne
    @JoinColumn(name = "termin_id") //Ovako odredjujemo kako ce se kolona sacuvati u tabeli
    private Termin termin;
}
