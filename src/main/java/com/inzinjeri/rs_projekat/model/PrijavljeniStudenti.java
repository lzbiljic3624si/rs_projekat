package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="prijavljeniStudenti", uniqueConstraints = @UniqueConstraint(columnNames = {"Student", "Termin", "Grupa"}))
public class PrijavljeniStudenti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "termin_id")
    private Termin termin;

    @ManyToOne
    @JoinColumn(name = "grupa_id")
    private Grupa grupa;

    @Column(nullable = false)
    private String status;
}
