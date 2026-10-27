package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RokRequest {
    @NotBlank
    private String naziv;
    @NotNull
    private Integer skolskaGodina;
}
