package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZadatakRequest {
    @NotBlank
    private String naziv;
    @NotNull
    private Long grupaId;
    @NotBlank
    private String repoPutanja;
}
