package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="prijavljenStudent", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "grupa_id"}))
public class PrijavljenStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "grupa_id")
    private Grupa grupa;
}
