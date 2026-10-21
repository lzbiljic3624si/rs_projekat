package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

@Data
public class ZadatakResponse {
    private Long id;
    private String naziv;
    private GrupaResponse grupa;
    private String repoPutanja;
}
