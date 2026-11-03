package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "komentar")
public class Komentar {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Size(max = 2000)
    private String komentar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rad_id", nullable = false)
    private Rad rad;


    @Column(name = "putanja_fajla", nullable = false)
    @NotBlank
    @Size(max = 500)
    private String putanjaFajla;

    @Column(name = "broj_linije", nullable = false)
    @NotNull
    @Positive
    private Integer brojLinije;
}
