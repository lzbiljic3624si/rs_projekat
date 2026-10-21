package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

@Data
public class RadResponse {
    private Long id;
    private StudentResponse student;
    private ZadatakResponse zadatak;
    private String repoPutanjaResenja;
    private Double poeni;
    private String status;
}
