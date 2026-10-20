package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="predajePredmet", uniqueConstraints = @UniqueConstraint(columnNames = {"profesor_id", "predmet_id"}))
public class PredajePredmet {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;

    @ManyToOne
    @JoinColumn(name = "predmet_id")
    private Predmet predmet;
}
