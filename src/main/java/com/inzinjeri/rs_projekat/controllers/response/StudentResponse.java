package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

@Data
public class StudentResponse {
    private Long id;
    private String ime;
    private String prezime;
    private String brIndeksa;
    private Integer godUpisa;
    private String email;
}
