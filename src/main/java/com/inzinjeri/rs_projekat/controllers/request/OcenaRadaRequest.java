package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OcenaRadaRequest {
    @NotNull
    @DecimalMin("0.0")
    private Double poeni;
}
