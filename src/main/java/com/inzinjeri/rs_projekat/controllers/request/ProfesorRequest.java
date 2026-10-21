package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfesorRequest {

    @NotBlank
    private String ime;
    @NotBlank
    private String prezime;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
