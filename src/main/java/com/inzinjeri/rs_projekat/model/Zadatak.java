package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.mapping.ToOne;

@Entity
@Data
@Table(name="zadatak")
public class Zadatak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    @OneToOne
    @JoinColumn(name = "grupa_id", unique = true)
    private Grupa grupa;

    @Column(nullable = false, unique = true)
    private String repoPutanja;
}
