package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrupaRequest {
    @NotBlank
    private String naziv;
    @NotBlank
    private String nazivPredmeta;
    @NotBlank
    private String nazivRoka;
    @NotBlank
    private String termin;
}
