package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name="PredajePredmet")
@ToString
public class PredajePredmet {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Id
    private Long ProfesorID;
    @Id
    private Long PredmetID;
}
