package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="rad", uniqueConstraints = @UniqueConstraint(columnNames = {"student", "zadatak_id"}))
public class Rad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "zadatak_id")
    private Zadatak zadatak;

    private LocalDateTime vremePreuzimanja;
    private LocalDateTime vremePredaje;
    private String repoPutanjaResenja;
    private Double poeni;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // Zadrzava postojecu tekstualnu kolonu umesto MySQL ENUM tipa.
    @Column(nullable = false)
    private StatusRada status;
}
