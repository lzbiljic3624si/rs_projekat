package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PredmetRequest {
    @NotBlank
    private String naziv;
    @NotBlank
    private String jezik;
    private String uslov;
}
