package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class StudentRequest {
    @NotBlank
    @Size(max = 255)
    private String ime;
    @NotBlank
    @Size(max = 255)
    private String prezime;
    @NotBlank
    @Pattern(regexp = "[A-Za-z]{1,20}")
    private String smer;
    @NotBlank
    @Pattern(regexp = "[0-9]{1,6}/[0-9]{4}")
    private String brIndeksa;
    @NotNull
    @Min(1900)
    @Max(9999)
    private Integer godUpisa;
}
