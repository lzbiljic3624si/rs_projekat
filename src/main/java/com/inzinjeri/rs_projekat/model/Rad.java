package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="rad", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student", "zadatak_id"}),
        @UniqueConstraint(name = "uk_rad_github_folder", columnNames = {"github_repo", "github_folder"})})
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
    @Column(length = 1000)
    private String repoPutanjaResenja;
    @Column(length = 140)
    private String githubRepo;
    @Column(length = 500)
    private String githubFolder;
    @Column(length = 64)
    private String githubCommit;
    @Column(length = 64)
    private String izvorniCommit;
    private Double poeni;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // Zadrzava postojecu tekstualnu kolonu umesto MySQL ENUM tipa.
    @Column(nullable = false)
    private StatusRada status;
}
