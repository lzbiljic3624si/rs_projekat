package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="Grupa")
@ToString
public class Grupa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String Naziv;
    @Column(nullable = false)
    private Long TerminID;
}
