package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfesorRequest {

    @NotBlank
    @Size(max = 255)
    private String ime;
    @NotBlank
    @Size(max = 255)
    private String prezime;
}
