package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PromenaLozinkeRequest {
    @NotBlank
    @Size(max = 72)
    private String staraLozinka;
    @NotBlank
    @Size(min = 8, max = 72)
    private String novaLozinka;
}
