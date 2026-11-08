package com.inzinjeri.rs_projekat.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IzmenaFajlaRequest {
    @NotBlank @Size(max = 500)
    private String putanja;
    @NotBlank @Pattern(regexp = "[a-fA-F0-9]{40}|[a-fA-F0-9]{64}")
    private String prethodniSha;
    @NotNull @Size(max = 2097152)
    private String tekst;
}
