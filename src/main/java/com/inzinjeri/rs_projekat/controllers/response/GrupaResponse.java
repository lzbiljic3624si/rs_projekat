package com.inzinjeri.rs_projekat.controllers.response;


import com.inzinjeri.rs_projekat.model.Termin;
import lombok.Data;

@Data
public class GrupaResponse {
    private Long id;
    private String naziv;
    private TerminResponse termin;
}
