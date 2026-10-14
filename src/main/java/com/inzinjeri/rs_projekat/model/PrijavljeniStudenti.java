package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="PrijavljeniStudenti")
@ToString
public class PrijavljeniStudenti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long StudentID;
    @Column(nullable = false)
    private Long TerminID;
    @Column(nullable = false)
    private Long GrupaID;
    @Column(nullable = false)
    private String Status;
}
