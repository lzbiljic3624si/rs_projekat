package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RadResponse {
    private Long id;
    private StudentResponse student;
    private ZadatakResponse zadatak;
    private String repoPutanjaResenja;
    private Double poeni;
    private String status;
    private LocalDateTime vremePreuzimanja;
    private LocalDateTime vremePredaje;
}
