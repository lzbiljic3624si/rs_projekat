package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="ucionica")
public class Ucionica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String Naziv;
}
