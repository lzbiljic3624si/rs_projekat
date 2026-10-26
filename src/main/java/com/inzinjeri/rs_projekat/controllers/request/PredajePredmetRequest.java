package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PredajePredmetRequest {
    @NotBlank
    private String nazivPredmeta;

    @NotBlank
    @Email
    private String emailProfesora;
}
