package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class StudentRequest {
    @NotBlank
    private String ime;
    @NotBlank
    private String prezime;
    @NotBlank
    private String smer;
    @NotBlank
    private String brIndeksa;
    @NotNull
    private Integer godUpisa;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
