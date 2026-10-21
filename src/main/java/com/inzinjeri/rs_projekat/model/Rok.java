package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="rok", uniqueConstraints = @UniqueConstraint(columnNames = {"naziv", "skolskaGodina"}))
public class Rok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;
    @Column(nullable = false)
    private Integer skolskaGodina;
}
