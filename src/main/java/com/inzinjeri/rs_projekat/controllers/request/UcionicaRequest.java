package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UcionicaRequest {
    @NotBlank
    private String naziv;
}
