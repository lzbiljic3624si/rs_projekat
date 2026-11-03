package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KomentarRequest {

    @NotBlank
    @Size(max = 2000)
    private String komentar;

    @NotBlank
    @Size(max = 500)
    private String putanjaFajla;

    @NotNull
    @Positive
    private Integer brojLinije;
}
