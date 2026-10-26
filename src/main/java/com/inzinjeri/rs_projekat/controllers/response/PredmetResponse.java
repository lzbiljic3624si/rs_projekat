package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

@Data
public class PredmetResponse {
    private Long id;
    private String naziv;
    private String jezik;
    private String uslov;
}
